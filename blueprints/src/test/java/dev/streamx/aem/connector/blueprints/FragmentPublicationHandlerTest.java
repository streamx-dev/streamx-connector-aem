package dev.streamx.aem.connector.blueprints;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.streamx.blueprints.data.Fragment;
import dev.streamx.sling.connector.PublishData;
import dev.streamx.sling.connector.StreamxPublicationException;
import dev.streamx.sling.connector.UnpublishData;
import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Random;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.engine.SlingRequestProcessor;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith({AemContextExtension.class, MockitoExtension.class})
class FragmentPublicationHandlerTest {

  private final AemContext context = new AemContext(ResourceResolverType.JCR_OAK);

  private static class BasicRequestProcessor implements SlingRequestProcessor {

    private static final int DATA_SIZE = 888;

    @Override
    public void processRequest(
        HttpServletRequest request, HttpServletResponse response, ResourceResolver resourceResolver
    ) throws IOException {
      String requestURI = request.getRequestURI();
      if (requestURI.equals("/content/experience-fragments/fragment.html")) {
        response.setContentType("application/octet-stream");
        response.setContentLength(DATA_SIZE);
        byte[] randomData = new byte[DATA_SIZE];
        new Random(0L).nextBytes(randomData);
        try (OutputStream out = response.getOutputStream()) {
          out.write(randomData);
          out.flush();
        }
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
    context.load().json(
        "/dev/streamx/aem/connector/blueprints/usual-aem-page.json",
        "/content/experience-fragments/fragment"
    );
    context.build().resource("/content/random-page").commit();
  }

  @SuppressWarnings("resource")
  @Test
  void mustHandle() throws StreamxPublicationException {
    String pagePath = "/content/usual-aem-page";
    String fragmentPath = "/content/experience-fragments/fragment";
    String expectedKey = "/content/experience-fragments/fragment.html";
    FragmentPublicationHandler handler = context.registerInjectActivateService(
        FragmentPublicationHandler.class
    );
    @SuppressWarnings("MagicNumber")
    int expectedLength = 1625;
    PublishData<Fragment> publishData = handler.getPublishData(fragmentPath);
    int actualLength = publishData.getModel().getContent().array().length;
    UnpublishData<Fragment> unpublishData = handler.getUnpublishData(fragmentPath);
    assertAll(
        () -> assertNotNull(context.resourceResolver().getResource(pagePath)),
        () -> assertFalse(handler.canHandle(pagePath)),
        () -> assertTrue(handler.canHandle(fragmentPath)),
        () -> assertEquals(expectedLength, actualLength),
        () -> assertEquals(expectedKey, publishData.getKey()),
        () -> assertEquals(expectedKey, unpublishData.getKey())
    );
  }
}
