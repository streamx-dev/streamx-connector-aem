package dev.streamx.aem.connector.blueprints;

import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;

import dev.streamx.sling.connector.ResourceInfo;
import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;
import java.lang.annotation.Annotation;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
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

  private final SlingRequestProcessor basicRequestProcessor = (HttpServletRequest request, HttpServletResponse response, ResourceResolver resolver) -> {
    String requestURI = request.getRequestURI();
    if (requestURI.equals("/content/franklin-page.plain.html")) {
      response.getWriter().write("<html><body>"
                                 + "<h1><a href='http://www.franklin-aem.com'>Franklin Page</a></h1>"
                                 + "<a href='http://www.franklin-roosevelt.com'>Franklin Roosevelt Page</a>"
                                 + "</body></html>");
    } else if (requestURI.equals("/content/usual-aem-page.html")) {
      response.getWriter().write("<html><body><h1>Usual AEM Page</h1></body></html>");
    } else {
      response.getWriter().write("<html><body><h1>Not Found</h1></body></html>");
    }
  };

  @BeforeEach
  void setup() {
    context.registerService(SlingRequestProcessor.class, basicRequestProcessor);
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
    assertThat(randomMarkup).isEqualTo("<html><body><h1>Not Found</h1></body></html>");
  }

  @Test
  void mustCheckIfPage() {
    PageDataService pageDataService = requireNonNull(context.getService(PageDataService.class));
    ResourceResolver resourceResolver = context.resourceResolver();
    ResourceInfo franklinPageResource = new ResourceInfo("/content/franklin-page", "cq:Page");
    ResourceInfo usualAEMPageResource = new ResourceInfo("/content/usual-aem-page", "cq:Page");
    ResourceInfo randomPageResource = new ResourceInfo("/blogs/random-page", "cq:Page");
    assertThat(pageDataService.isPage(franklinPageResource, resourceResolver)).isTrue();
    assertThat(pageDataService.isPage(usualAEMPageResource, resourceResolver)).isTrue();
    assertThat(pageDataService.isPage(randomPageResource, resourceResolver)).isFalse();
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

    PageDataService pageDataService = new PageDataService(basicRequestProcessor, config);
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
