package dev.streamx.aem.connector.blueprints;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.engine.SlingRequestProcessor;
import org.apache.sling.servlethelpers.internalrequests.SlingInternalRequest;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(service = PageDataService.class)
@Designate(ocd = PageDataServiceConfig.class)
public class PageDataService {

  private static final Logger LOG = LoggerFactory.getLogger(PageDataService.class);

  @Reference
  private SlingRequestProcessor requestProcessor;

  private String pagesPathRegexp;
  private String templatesPathRegexp;
  private boolean shouldShortenContentPaths;
  private boolean shouldAddNofollowToExternalLinks;
  private HashSet<String> nofollowHostsToSkip;

  @Activate
  @Modified
  private void activate(PageDataServiceConfig config) {
    pagesPathRegexp = config.pages_path_regexp();
    templatesPathRegexp = config.templates_path_regexp();
    shouldShortenContentPaths = config.shorten_content_paths();
    shouldAddNofollowToExternalLinks = config.nofollow_external_links();
    nofollowHostsToSkip = new HashSet<>(Arrays.asList(config.nofollow_hosts_to_skip()));
  }

  public InputStream getStorageData(Resource resource) throws IOException {
    String pageMarkup = new SlingInternalRequest(resource.getResourceResolver(), requestProcessor,
        resource.getPath())
        .withExtension("html")
        .withParameter("wcmmode", "disabled")
        .execute()
        .getResponseAsString();

    pageMarkup = addNoFollowToExternalLinksIfNeeded(resource.getPath(), pageMarkup);

    return wrapStreamIfNeeded(resource.getPath(), new ByteArrayInputStream(pageMarkup.getBytes()));
  }

  public boolean isPage(String resourcePath) {
    return isMatchingPagesPathPattern(resourcePath) && !isMatchingPageTemplatePattern(resourcePath);
  }

  public boolean isPageTemplate(String resourcePath) {
    return isMatchingPagesPathPattern(resourcePath) && isMatchingPageTemplatePattern(resourcePath);
  }

  private boolean isMatchingPageTemplatePattern(String resourcePath) {
    return resourcePath.matches(templatesPathRegexp);
  }

  private boolean isMatchingPagesPathPattern(String resourcePath) {
    return resourcePath.matches(pagesPathRegexp);
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
