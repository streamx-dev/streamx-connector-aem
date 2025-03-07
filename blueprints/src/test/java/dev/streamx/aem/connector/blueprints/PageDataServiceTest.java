package dev.streamx.aem.connector.blueprints;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.io.IOUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.engine.SlingRequestProcessor;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith({AemContextExtension.class, MockitoExtension.class})
class PageDataServiceTest {

  @SuppressWarnings({"ProtectedField", "VisibilityModifier"})
  protected final AemContext context = new AemContext(ResourceResolverType.JCR_OAK);

  private static class BasicRequestProcessor implements SlingRequestProcessor {
    @Override
    public void processRequest(
        HttpServletRequest request, HttpServletResponse response, ResourceResolver resourceResolver
    ) throws IOException {
      String requestURI = request.getRequestURI();
      if (requestURI.equals("/content/franklin-page.plain.html")) {
        response.getWriter().write("<html><body><h1>Franklin Page</h1></body></html>");
      } else if (requestURI.equals("/content/usual-aem-page.html")) {
        response.getWriter().write("<html><body><h1>Usual AEM Page</h1></body></html>");
      } else {
        response.getWriter().write("<html><body><h1>Not Found</h1></body></html>");
      }
    }
  }

  @BeforeEach
  void setup() {
    context.registerService(SlingRequestProcessor.class, new BasicRequestProcessor());
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
  void mustReturnValidMarkup() throws IOException {
    PageDataService pageDataService = Optional.ofNullable(context.getService(PageDataService.class))
        .orElseThrow();
    @SuppressWarnings("resource")
    ResourceResolver resourceResolver = context.resourceResolver();
    Resource franklinPageResource = Optional.ofNullable(
        resourceResolver.getResource("/content/franklin-page")
    ).orElseThrow();
    Resource usualAEMPageResource = Optional.ofNullable(
        resourceResolver.getResource("/content/usual-aem-page")
    ).orElseThrow();
    Resource randomPageResource = Optional.ofNullable(
        resourceResolver.getResource("/content/random-page")
    ).orElseThrow();
    InputStream franklinIS = pageDataService.getStorageData(franklinPageResource);
    InputStream usualAEMIS = pageDataService.getStorageData(usualAEMPageResource);
    InputStream randomIS = pageDataService.getStorageData(randomPageResource);
    String franklinMarkup = IOUtils.toString(franklinIS, StandardCharsets.UTF_8);
    String usualAEMMarkup = IOUtils.toString(usualAEMIS, StandardCharsets.UTF_8);
    String randomMarkup = IOUtils.toString(randomIS, StandardCharsets.UTF_8);
    assertAll(
        () -> assertEquals("<html><body><h1>Franklin Page</h1></body></html>", franklinMarkup),
        () -> assertEquals("<html><body><h1>Usual AEM Page</h1></body></html>", usualAEMMarkup),
        () -> assertEquals("<html><body><h1>Not Found</h1></body></html>", randomMarkup)
    );
  }

  @Test
  void mustCheckIfPage() {
    PageDataService pageDataService = Optional.ofNullable(context.getService(PageDataService.class))
        .orElseThrow();
    @SuppressWarnings("resource")
    ResourceResolver resourceResolver = context.resourceResolver();
    Resource franklinPageResource = Optional.ofNullable(
        resourceResolver.getResource("/content/franklin-page")
    ).orElseThrow();
    Resource usualAEMPageResource = Optional.ofNullable(
        resourceResolver.getResource("/content/usual-aem-page")
    ).orElseThrow();
    Resource randomPageResource = Optional.ofNullable(
        resourceResolver.getResource("/content/random-page")
    ).orElseThrow();
    assertAll(
        () -> assertTrue(pageDataService.isPage(franklinPageResource.getPath())),
        () -> assertTrue(pageDataService.isPage(usualAEMPageResource.getPath())),
        () -> assertFalse(pageDataService.isPage(randomPageResource.getPath()))
    );
  }
}
