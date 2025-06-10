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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
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
  private static final Map<String, String> additionalInternalRequestProperties = Map.of("resolveStreamxDirectives", "true");

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
        resource, resourceResolver, slingRequestProcessor, additionalInternalRequestProperties
    );

    String pageMarkupWithAdjustedLinks = shouldAddNofollowToExternalLinks
        ? addNoFollowToExternalLinks(resourcePath, pageMarkup)
        : pageMarkup;

    return shouldShortenContentPaths
        ? shortenContentPaths(resourcePath, pageMarkupWithAdjustedLinks)
        : pageMarkupWithAdjustedLinks;
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

  private String addNoFollowToExternalLinks(String pagePath, String pageMarkup) {
    try {
      Document document = Jsoup.parse(pageMarkup, UTF_8.name());
      Elements links = document.select("a[href]");
      for (Element link : links) {
        String href = link.attr("href");
        if (!StringUtils.startsWith(href, "/") && isNofollowAllowedForHost(href)) {
          link.attr("rel", "nofollow");
        }
      }
      return document.outerHtml();
    } catch (Exception e) {
      LOG.warn("Cannot add 'nofollow' attributes for page: [{}]. Original content will be used.",
          pagePath);
    }
    return pageMarkup;
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

  protected static String shortenContentPaths(String path, String content) {
    if (!path.startsWith("/content")) {
      return content;
    }

    final String[] elements = path.split("/");
    final String spaceName = elements.length >= 3 ? elements[2] : null;
    if (spaceName != null) {
      Map<String, String> replaces = new LinkedHashMap<>();
      // TODO this is a quick workaround to be able to use Pebble in AEM
      replaces.put("((", "{{");
      replaces.put("))", "}}");
      // Replace pages paths
      replaces.put("/content/" + spaceName + "/pages", null);
      // Replace generic paths
      replaces.put("/content/" + spaceName, null);
      try {
        return replaceTokens(content, replaces);
      } catch (IOException exception) {
        String message = String.format("Error modifying content for %s", path);
        throw new UncheckedIOException(message, exception);
      }
    }
    return content;
  }

  private static String replaceTokens(String input, Map<String, String> replaces) throws IOException {
    InputStream inputStream = new ByteArrayInputStream(input.getBytes(UTF_8));
    for (Entry<String, String> entry : replaces.entrySet()) {
      String pattern = entry.getKey();
      String replacement = entry.getValue();
      inputStream = new ReplacingInputStream(inputStream, pattern, replacement);
    }

    return IOUtils.toString(inputStream, UTF_8);
  }

}
