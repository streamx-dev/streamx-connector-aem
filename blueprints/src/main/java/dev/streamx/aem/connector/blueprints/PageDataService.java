package dev.streamx.aem.connector.blueprints;

import static java.nio.charset.StandardCharsets.UTF_8;

import dev.streamx.sling.connector.ResourceInfo;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.engine.SlingRequestProcessor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.propertytypes.ServiceDescription;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(service = PageDataService.class)
@Designate(ocd = PageDataServiceConfig.class)
@ServiceDescription("Operations on page data")
public class PageDataService {

  private static final Logger LOG = LoggerFactory.getLogger(PageDataService.class);

  private final SlingRequestProcessor slingRequestProcessor;
  private String pagesPathRegexp;
  private String templatesPathRegexp;
  private boolean shouldShortenContentPaths;
  private boolean shouldAddNofollowToExternalLinks;
  private Set<String> nofollowHostsToSkip;

  @Activate
  public PageDataService(
      @Reference(cardinality = ReferenceCardinality.MANDATORY)
      SlingRequestProcessor slingRequestProcessor,
      PageDataServiceConfig config
  ) {
    this.slingRequestProcessor = slingRequestProcessor;
    configure(config);
  }

  @Modified
  private void configure(PageDataServiceConfig config) {
    pagesPathRegexp = config.pages_path_regexp();
    templatesPathRegexp = config.templates_path_regexp();
    shouldShortenContentPaths = config.shorten_content_paths();
    shouldAddNofollowToExternalLinks = config.nofollow_external_links();
    nofollowHostsToSkip = new HashSet<>(Arrays.asList(config.nofollow_hosts_to_skip()));
  }

  public String getStorageData(Resource resource, ResourceResolver resourceResolver) {
    String resourcePath = resource.getPath();
    String pageMarkup = InternalRequestForPage.generateMarkup(
        resource, resourceResolver, slingRequestProcessor
    );

    String pageMarkupWithAdjustedLinks = addNoFollowToExternalLinksIfNeeded(
        resourcePath, pageMarkup
    );

    if (shouldShortenContentPaths && resourcePath.startsWith("/content")) {
      return shortenContentPaths(resourcePath, pageMarkupWithAdjustedLinks);
    } else {
      return pageMarkupWithAdjustedLinks;
    }
  }

  boolean isPage(ResourceInfo resource, ResourceResolver resourceResolver) {
    boolean isPage = ResourcePrimaryNodeTypeChecker.isPage(resource, resourceResolver)
                     && resource.getPath().matches(pagesPathRegexp);
    LOG.trace("Is {} a page? Answer: {}", resource.getPath(), isPage);
    return isPage;
  }

  boolean isPageTemplate(ResourceInfo resource, ResourceResolver resourceResolver) {
    boolean isPageTemplate = ResourcePrimaryNodeTypeChecker.isPage(resource, resourceResolver)
                             && resource.getPath().matches(templatesPathRegexp);
    LOG.trace("Is {} a page template? Answer: {}", resource.getPath(), isPageTemplate);
    return isPageTemplate;
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
    Document document = Jsoup.parse(html, UTF_8.name());
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

  private String shortenContentPaths(String path, String content) {
    final String[] elements = path.split("/");
    final String spaceName = elements.length >= 2 ? elements[2] : null;
    if (spaceName != null) {
      final String pagesPath = "/content/" + spaceName + "/pages";
      final String contentPath = "/content/" + spaceName;
      // TODO this is a quick workaround to be able to use Pebble in AEM
      InputStream input = new ByteArrayInputStream(content.getBytes(UTF_8));
      final InputStream replacingOpeningStream =
          new ReplacingInputStream(input, "((", "{{");
      final InputStream replacingClosingStream =
          new ReplacingInputStream(replacingOpeningStream, "))", "}}");

      // Replace pages paths
      final InputStream replacePages =
          new ReplacingInputStream(replacingClosingStream, pagesPath, null);
      // Replace generic paths
      ReplacingInputStream replaceGenericPaths = new ReplacingInputStream(replacePages,
          contentPath, null);
      try {
        return IOUtils.toString(replaceGenericPaths, UTF_8);
      } catch (IOException exception) {
        String message = String.format("Error modifying content for %s", path);
        throw new UncheckedIOException(message, exception);
      }
    }
    return content;
  }

}
