package dev.streamx.aem.connector.blueprints;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.uri.SlingUri;
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
  private final ResourceResolverFactory resourceResolverFactory;
  private String pagesPathRegexp;
  private String templatesPathRegexp;
  private boolean shouldShortenContentPaths;
  private boolean shouldAddNofollowToExternalLinks;
  private Set<String> nofollowHostsToSkip;

  @Activate
  public PageDataService(
      @Reference(cardinality = ReferenceCardinality.MANDATORY)
      SlingRequestProcessor slingRequestProcessor,
      @Reference(cardinality = ReferenceCardinality.MANDATORY)
      ResourceResolverFactory resourceResolverFactory,
      PageDataServiceConfig config
  ) {
    this.slingRequestProcessor = slingRequestProcessor;
    this.resourceResolverFactory = resourceResolverFactory;
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

  public InputStream getStorageData(Resource resource) throws IOException {
    String resourcePath = resource.getPath();
    String pageMarkup = new InternalRequestForPage(
        resourceResolverFactory, resource, slingRequestProcessor
    ).generateMarkup();

    String pageMarkupWithAdjustedLinks = addNoFollowToExternalLinksIfNeeded(
        resourcePath, pageMarkup
    );

    return wrapStreamIfNeeded(
        resourcePath, new ByteArrayInputStream(pageMarkupWithAdjustedLinks.getBytes())
    );
  }

  boolean isPage(SlingUri slingUri) {
    boolean isPage = new PageCandidate(resourceResolverFactory, slingUri, pagesPathRegexp).isPage();
    LOG.trace("Is {} a page? Answer: {}", slingUri, isPage);
    return isPage;
  }

  boolean isPageTemplate(String resourcePath) {
    SlingUri slingUri = new DefaultSlingUriBuilder(resourcePath, resourceResolverFactory).build();
    boolean isPageTemplate = new PageCandidate(
        resourceResolverFactory, slingUri, templatesPathRegexp
    ).isPage();
    LOG.trace("Is {} a page template? Answer: {}", resourcePath, isPageTemplate);
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
