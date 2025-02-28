package dev.streamx.aem.connector.blueprints;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import dev.streamx.blueprints.data.Asset;
import dev.streamx.sling.connector.PublishData;
import dev.streamx.sling.connector.StreamxPublicationException;
import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;
import java.util.Random;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.engine.SlingRequestProcessor;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(AemContextExtension.class)
class ReferencedAssetHandlerTest {

  private static final int DATA_SIZE = 3024;

  private final AemContext context = new AemContext(ResourceResolverType.JCR_OAK);

  private static class BasicRequestProcessor implements SlingRequestProcessor {

    @SuppressWarnings({"MagicNumber", "IfCanBeSwitch", "IfStatementWithTooManyBranches"})
    @Override
    public void processRequest(
        HttpServletRequest request, HttpServletResponse response, ResourceResolver resourceResolver
    ) throws IOException {
      String requestURI = request.getRequestURI();
      if (requestURI.equals("/content/firsthops/us/en/_jcr_content/root/container/container/image_1057652191.coreimg.85.1024.jpeg/1740144616999/lava-rock-formation.jpeg")) {
        writeInto(response, 1024);
      } else if (requestURI.equals("/content/firsthops/us/en/_jcr_content/root/container/container/image_1057652191.coreimg.85.1200.jpeg/1740144616999/lava-rock-formation.jpeg")) {
        writeInto(response, 1200);
      } else if (requestURI.equals("/content/firsthops/us/en/_jcr_content/root/container/container/image_1057652191.coreimg.85.1600.jpeg/1740144616999/lava-rock-formation.jpeg")) {
        writeInto(response, 1600);
      } else if (requestURI.equals("/content/firsthops/us/en/_jcr_content/root/container/container/image_1057652191.coreimg.85.320.jpeg/1740144616999/lava-rock-formation.jpeg")) {
        writeInto(response, 320);
      } else if (requestURI.equals("/content/firsthops/us/en/_jcr_content/root/container/container/image_1057652191.coreimg.85.480.jpeg/1740144616999/lava-rock-formation.jpeg")) {
        writeInto(response, 480);
      } else if (requestURI.equals("/content/firsthops/us/en/_jcr_content/root/container/container/image_1057652191.coreimg.85.600.jpeg/1740144616999/lava-rock-formation.jpeg")) {
        writeInto(response, 600);
      } else if (requestURI.equals("/content/firsthops/us/en/_jcr_content/root/container/container/image_1057652191.coreimg.85.800.jpeg/1740144616999/lava-rock-formation.jpeg")) {
        writeInto(response, 800);
      } else if (requestURI.equals("/content/firsthops/us/en/_jcr_content/root/container/container/image_1057652191.coreimg.jpeg/1740144616999/lava-rock-formation.jpeg")) {
        writeInto(response, DATA_SIZE);
      } else {
        response.setContentType("text/html");
        response.getWriter().write("<html><body><h1>Not Found</h1></body></html>");
      }
    }

    private void writeInto(ServletResponse response, int dataSize) throws IOException {
      response.setContentType("application/octet-stream");
      response.setContentLength(dataSize);
      byte[] randomData = new byte[dataSize];
      new Random().nextBytes(randomData);
      try (OutputStream out = response.getOutputStream()) {
        out.write(randomData);
        out.flush();
      }
    }
  }

  @BeforeEach
  void setup() {
    context.registerService(SlingRequestProcessor.class, new BasicRequestProcessor());
    context.load().json(
        "/dev/streamx/aem/connector/blueprints/sample-assets.json",
        "/content/dam/core-components-examples/library/sample-assets"
    );
    context.build().resource("/conf/irrelevant-resource").commit();
  }

  @Test
  void dontHandleUsualAssets() {
    String mountainPath = "/content/dam/core-components-examples/library/sample-assets/mountain-range.jpg";
    String mountainContent = "/content/dam/core-components-examples/library/sample-assets/mountain-range.jpg/jcr:content";
    String irrelevantPath = "/conf/irrelevant-resource";
    @SuppressWarnings("resource")
    ResourceResolver resourceResolver = context.resourceResolver();
    ReferencedAssetHandler enabled = context.registerInjectActivateService(
        ReferencedAssetHandler.class, Map.of("enabled", true)
    );
    ReferencedAssetHandler disabled = context.registerInjectActivateService(
        ReferencedAssetHandler.class, Map.of("enabled", false)
    );
    assertAll(
        () -> assertNotNull(resourceResolver.getResource(irrelevantPath)),
        () -> assertNotNull(resourceResolver.getResource(mountainPath)),
        () -> assertNotNull(resourceResolver.getResource(mountainContent)),
        () -> assertFalse(enabled.canHandle(mountainPath)),
        () -> assertFalse(disabled.canHandle(mountainPath)),
        () -> assertFalse(enabled.canHandle(irrelevantPath)),
        () -> assertFalse(enabled.canHandle(mountainContent))
    );
  }

  @Test
  void canGetPublishData() {
    @SuppressWarnings("MagicNumber")
    Map<String, Integer> assetPaths = Map.of(
        "/content/firsthops/us/en/_jcr_content/root/container/container/image_1057652191.coreimg.85.1024.jpeg/1740144616999/lava-rock-formation.jpeg",
        1024,
        "/content/firsthops/us/en/_jcr_content/root/container/container/image_1057652191.coreimg.85.1200.jpeg/1740144616999/lava-rock-formation.jpeg",
        1200,
        "/content/firsthops/us/en/_jcr_content/root/container/container/image_1057652191.coreimg.85.1600.jpeg/1740144616999/lava-rock-formation.jpeg",
        1600,
        "/content/firsthops/us/en/_jcr_content/root/container/container/image_1057652191.coreimg.85.320.jpeg/1740144616999/lava-rock-formation.jpeg",
        320,
        "/content/firsthops/us/en/_jcr_content/root/container/container/image_1057652191.coreimg.85.480.jpeg/1740144616999/lava-rock-formation.jpeg",
        480,
        "/content/firsthops/us/en/_jcr_content/root/container/container/image_1057652191.coreimg.85.600.jpeg/1740144616999/lava-rock-formation.jpeg",
        600,
        "/content/firsthops/us/en/_jcr_content/root/container/container/image_1057652191.coreimg.85.800.jpeg/1740144616999/lava-rock-formation.jpeg",
        800,
        "/content/firsthops/us/en/_jcr_content/root/container/container/image_1057652191.coreimg.jpeg/1740144616999/lava-rock-formation.jpeg",
        DATA_SIZE
    );
    ReferencedAssetHandler handler = context.registerInjectActivateService(
        ReferencedAssetHandler.class, Map.of("enabled", true)
    );
    assetPaths.forEach(
        (assetPath, expectedSize) -> {
          PublishData<Asset> publishData;
          try {
            publishData = handler.getPublishData(assetPath);
          } catch (StreamxPublicationException e) {
            throw new RuntimeException(e);
          }
          int length = publishData.getModel().getContent().array().length;
          String key = publishData.getKey();
          Asset model = publishData.getModel();
          String channel = publishData.getChannel();
          assertAll(
              () -> assertEquals(expectedSize, length),
              () -> assertEquals(key, assetPath),
              () -> assertEquals("assets", channel),
              () -> assertEquals(Asset.class, model.getClass())
          );
        }
    );
  }

}
