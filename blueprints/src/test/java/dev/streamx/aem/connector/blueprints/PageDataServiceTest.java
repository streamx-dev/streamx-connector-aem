package dev.streamx.aem.connector.blueprints;

import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;

import dev.streamx.aem.connector.test.util.FixedResponseSlingRequestProcessor;
import dev.streamx.sling.connector.ResourceInfo;
import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;
import java.lang.annotation.Annotation;
import java.util.Map;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.engine.SlingRequestProcessor;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(AemContextExtension.class)
class PageDataServiceTest {

  private final AemContext context = new AemContext(ResourceResolverType.JCR_OAK);

  private final SlingRequestProcessor requestProcessor = new FixedResponseSlingRequestProcessor(
      Map.of(
          "/content/franklin-page.plain.html",
          "<html><body>"
            + "<h1><a href='http://www.franklin-aem.com'>Franklin Page</a></h1>"
            + "<a href='http://www.franklin-roosevelt.com'>Franklin Roosevelt Page</a>"
            + "</body></html>",

          "/content/usual-aem-page.html",
          "<html><body><h1>Usual AEM Page</h1></body></html>",

          "/content/random-page.html",
          "<html><body><h1>Random content</h1></body></html>"
      )
  );

  @BeforeEach
  void setup() {
    context.registerService(SlingRequestProcessor.class, requestProcessor);
    context.registerInjectActivateService(PageDataService.class);
    context.load().json(
        "/dev/streamx/aem/connector/blueprints/franklin-page.json",
        "/content/franklin-page"
    );
    context.load().json(
        "/dev/streamx/aem/connector/blueprints/usual-aem-page.json",
        "/content/usual-aem-page"
    );
    context.build().resource("/content/random-page").commit();
  }

  @Test
  void mustReturnValidMarkup() {
    PageDataService pageDataService = requireNonNull(context.getService(PageDataService.class));
    ResourceResolver resourceResolver = context.resourceResolver();
    Resource franklinPageResource = requireNonNull(resourceResolver.getResource("/content/franklin-page"));
    Resource usualAEMPageResource = requireNonNull(resourceResolver.getResource("/content/usual-aem-page"));
    Resource randomPageResource = requireNonNull(resourceResolver.getResource("/content/random-page"));
    String franklinMarkup = pageDataService.getStorageData(franklinPageResource, resourceResolver);
    String usualAEMMarkup = pageDataService.getStorageData(usualAEMPageResource, resourceResolver);
    String randomMarkup = pageDataService.getStorageData(randomPageResource, resourceResolver);
    assertThat(franklinMarkup).isEqualTo("<html><body><h1><a href='http://www.franklin-aem.com'>Franklin Page</a></h1><a href='http://www.franklin-roosevelt.com'>Franklin Roosevelt Page</a></body></html>");
    assertThat(usualAEMMarkup).isEqualTo("<html><body><h1>Usual AEM Page</h1></body></html>");
    assertThat(randomMarkup).isEqualTo("<html><body><h1>Random content</h1></body></html>");
  }

  @Test
  void mustCheckIfPageByResourcePath() {
    PageDataService pageDataService = requireNonNull(context.getService(PageDataService.class));
    ResourceInfo franklinPageResource = new ResourceInfo("/content/franklin-page", "cq:Page");
    ResourceInfo usualAEMPageResource = new ResourceInfo("/content/usual-aem-page", "cq:Page");
    ResourceInfo randomPageResource = new ResourceInfo("/blogs/random-page", "cq:Page");
    assertThat(pageDataService.isPageByResourcePath(franklinPageResource)).isTrue();
    assertThat(pageDataService.isPageByResourcePath(usualAEMPageResource)).isTrue();
    assertThat(pageDataService.isPageByResourcePath(randomPageResource)).isFalse();
  }

  @Test
  void shouldAddNofollowToExternalLinks() {
    PageDataServiceConfig config = new PageDataServiceConfig() {

      @Override
      public Class<? extends Annotation> annotationType() {
        return PageDataServiceConfig.class;
      }

      @Override
      public String pages_path_regexp() {
        return ".*";
      }

      @Override
      public String templates_path_regexp() {
        return ".*";
      }

      @Override
      public boolean shorten_content_paths() {
        return true;
      }

      @Override
      public boolean nofollow_external_links() {
        return true;
      }

      @Override
      public String[] nofollow_hosts_to_skip() {
        return new String[]{
            "www.franklin-roosevelt.com"
        };
      }
    };

    PageDataService pageDataService = new PageDataService(requestProcessor, config);
    ResourceResolver resourceResolver = context.resourceResolver();
    Resource franklinPageResource = requireNonNull(resourceResolver.getResource("/content/franklin-page"));
    String aemPageContent = pageDataService.getStorageData(franklinPageResource, resourceResolver);

    assertThat(normalizeHtml(aemPageContent)).isEqualTo(normalizeHtml(
        "<html>"
        + " <head></head>"
        + " <body>"
        + "  <h1><a href=\"http://www.franklin-aem.com\" rel=\"nofollow\">Franklin Page</a></h1>"
        + "  <a href=\"http://www.franklin-roosevelt.com\">Franklin Roosevelt Page</a>"
        + " </body>"
        + "</html>"
    ));
  }

  private static String normalizeHtml(String html) {
    return Jsoup.parse(html).html().trim();
  }
}
