package dev.streamx.aem.connector.blueprints;

import dev.streamx.sling.connector.PublicationHandler;
import dev.streamx.sling.connector.PublishData;
import dev.streamx.sling.connector.UnpublishData;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.engine.SlingRequestProcessor;
import org.apache.sling.servlethelpers.internalrequests.SlingInternalRequest;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(service = PublicationHandler.class)
@Designate(ocd = PagePublicationHandlerConfig.class)
public class PagePublicationHandler implements PublicationHandler<Data> {

  private static final Logger LOG = LoggerFactory.getLogger(PagePublicationHandler.class);

  private static final String ID = "streamx-page";
  private static final String DOT_HTML = ".html";
  private static final String PAGES_CHANNEL = "pages";
  private static final String TEMPLATES_CHANNEL = "templates";

  @Reference
  private SlingRequestProcessor requestProcessor;

  @Reference
  private ResourceResolverFactory resolverFactory;

  private boolean enabled;
  private String pagesPathRegexp;
  private String templatesPathRegexp;
  private boolean shouldShortenContentPaths;
  private boolean shouldAddNofollowToExternalLinks;
  private HashSet<String> nofollowHostsToSkip;

  private static String getPagePath(String resourcePath) {
    // StreamX demo expects published content keys to start with '/published', not with '/content'.
    String path = resourcePath + DOT_HTML;
    return "/published" + StringUtils.removeStart(path, "/content");
  }

  @Activate
  private void activate(PagePublicationHandlerConfig config) {
    enabled = config.enabled();
    pagesPathRegexp = config.pages_path_regexp();
    templatesPathRegexp = config.templates_path_regexp();
    shouldShortenContentPaths = config.shorten_content_paths();
    shouldAddNofollowToExternalLinks = config.nofollow_external_links();
    nofollowHostsToSkip = new HashSet<>(Arrays.asList(config.nofollow_hosts_to_skip()));
  }

  @Override
  public String getId() {
    return ID;
  }

  @Override
  public boolean canHandle(String resourcePath) {
    return enabled
        && (resourcePath.matches(pagesPathRegexp) || resourcePath.matches(templatesPathRegexp))
        && !resourcePath.contains("jcr:content");
  }

  @Override
  public PublishData<Data> getPublishData(String resourcePath) {
    try (ResourceResolver resourceResolver = createResourceResolver()) {
      Resource resource = resourceResolver.getResource(resourcePath);

      if (resource == null) {
        LOG.error("Resource not found when trying to publish it: {}", resourcePath);
        return null;
      }

      return new PublishData<>(
          getPagePath(resourcePath),
          getChannel(resourcePath),
          Data.class,
          getPageModel(resource));
    }
  }

  private ResourceResolver createResourceResolver() {
    try {
      return resolverFactory.getAdministrativeResourceResolver(null);
    } catch (LoginException e) {
      throw new IllegalStateException("Cannot create ResourceResolver", e);
    }
  }

  @Override
  public UnpublishData<Data> getUnpublishData(String resourcePath) {
    return new UnpublishData<>(
        getPagePath(resourcePath),
        getChannel(resourcePath),
        Data.class);
  }

  private String getChannel(String resourcePath) {
    if (resourcePath.matches(templatesPathRegexp)) {
      return TEMPLATES_CHANNEL;
    }
    return PAGES_CHANNEL;
  }

  private Data getPageModel(Resource resource) {
    try {
      String pageMarkup = renderPage(resource);
      pageMarkup = addNoFollowToExternalLinksIfNeeded(resource.getPath(), pageMarkup);
      InputStream inputStream = wrapStreamIfNeeded(resource.getPath(),
          new ByteArrayInputStream(pageMarkup.getBytes()));

      return new Data(ByteBuffer.wrap(IOUtils.toByteArray(inputStream)));
    } catch (IOException e) {
      throw new UncheckedIOException("Cannot create page model", e);
    }
  }

  private String renderPage(Resource resource) throws IOException {
    return new SlingInternalRequest(resource.getResourceResolver(), requestProcessor,
        resource.getPath())
        .withExtension("html")
        .withParameter("wcmmode", "disabled")
        .execute()
        .getResponseAsString();
  }

  private String addNoFollowToExternalLinksIfNeeded(String pagePath, String pageMarkup) {
    if (shouldAddNofollowToExternalLinks) {
      try {
        pageMarkup = addNofollowToExternalLinks(pageMarkup);
      } catch (Exception e) {
        LOG.warn("Cannot add 'nofollow' attributes for page: [{}]. Original content will be used.",
            pagePath);
      }
    }
    return pageMarkup;
  }

  private String addNofollowToExternalLinks(String html) {
    Document document = Jsoup.parse(html, StandardCharsets.UTF_8.name());
    Elements links = document.select("a[href]");
    for (Element link : links) {
      String href = link.attr("href");
      if (!StringUtils.startsWith(href, "/") && isNofollowAllowedForHost(href)) {
        link.attr("rel", "nofollow");
      }
    }
    return document.outerHtml();
  }

  private boolean isNofollowAllowedForHost(String href) {
    try {
      URI uri = new URI(href);
      return !nofollowHostsToSkip.contains(uri.getHost());
    } catch (URISyntaxException e) {
      LOG.debug("Cannot parse href {}: {}", href, e.getMessage());
      return true;
    }
  }

  private InputStream wrapStreamIfNeeded(String path, InputStream input) {
    if (shouldShortenContentPaths && path.startsWith("/content")) {
      final String[] elements = path.split("/");
      final String spaceName = elements.length >= 2 ? elements[2] : null;
      if (spaceName != null) {
        final byte[] pagesPath = ("/content/" + spaceName + "/pages").getBytes();
        final byte[] contentPath = ("/content/" + spaceName).getBytes();
        // TODO this is a quick workaround to be able to use Pebble in AEM
        final InputStream replacingOpeningStream =
            new ReplacingInputStream(input, "((".getBytes(), "{{".getBytes());
        final InputStream replacingClosingStream =
            new ReplacingInputStream(replacingOpeningStream, "))".getBytes(), "}}".getBytes());

        // Replace pages paths
        final InputStream replacePages =
            new ReplacingInputStream(replacingClosingStream, pagesPath, null);
        // Replace generic paths
        return new ReplacingInputStream(replacePages, contentPath, null);
      }
    }
    return input;
  }
}
