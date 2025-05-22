package dev.streamx.aem.connector.blueprints;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.streamx.blueprints.data.Asset;
import dev.streamx.sling.connector.PublishData;
import dev.streamx.sling.connector.ResourceInfo;
import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.engine.SlingRequestProcessor;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(AemContextExtension.class)
class AssetPublicationHandlerTest {

  private static final int DATA_SIZE = 3024;

  private final AemContext context = new AemContext(ResourceResolverType.JCR_OAK);

  private static class BasicRequestProcessor implements SlingRequestProcessor {

    @Override
    public void processRequest(
        HttpServletRequest request, HttpServletResponse response, ResourceResolver resourceResolver
    ) throws IOException {
      response.setContentType("text/html");
      response.getWriter().write("<html><body><h1>Not Found</h1></body></html>");
    }
  }

  @SuppressWarnings("resource")
  @BeforeEach
  void setup() throws PersistenceException {
    context.registerService(SlingRequestProcessor.class, new BasicRequestProcessor());
    context.load().json(
        "/dev/streamx/aem/connector/blueprints/sample-assets.json",
        "/content/dam/core-components-examples/library/sample-assets"
    );
    byte[] bufferBig = new byte[DATA_SIZE];
    Arrays.fill(bufferBig, NumberUtils.BYTE_ONE);
    context.resourceResolver().delete(
        Optional.ofNullable(
            context.resourceResolver().getResource(
                "/content/dam/core-components-examples/library/sample-assets/lava-rock-formation.jpg/jcr:content/renditions/original"
            )
        ).orElseThrow()
    );
    context.load().binaryFile(
        new ByteArrayInputStream(bufferBig),
        "/content/dam/core-components-examples/library/sample-assets/lava-rock-formation.jpg/jcr:content/renditions/original"
    );
    context.build().resource("/conf/irrelevant-resource").commit();
    context.build().resource("/conf/irrelevant-resource").commit();
  }

  @Test
  void canHandleUsualAssets() {
    String mountainPath = "/content/dam/core-components-examples/library/sample-assets/mountain-range.jpg";
    String mountainContent = "/content/dam/core-components-examples/library/sample-assets/mountain-range.jpg/jcr:content";
    String irrelevantPath = "/conf/irrelevant-resource";
    @SuppressWarnings("resource")
    ResourceResolver resourceResolver = context.resourceResolver();
    AssetPublicationHandler enabled = context.registerInjectActivateService(
        AssetPublicationHandler.class, Map.of("enabled", true)
    );
    AssetPublicationHandler disabled = context.registerInjectActivateService(
        AssetPublicationHandler.class, Map.of("enabled", false)
    );
    assertAll(
        () -> assertNotNull(resourceResolver.getResource(irrelevantPath)),
        () -> assertNotNull(resourceResolver.getResource(mountainPath)),
        () -> assertNotNull(resourceResolver.getResource(mountainContent)),
        () -> assertTrue(enabled.canHandle(new ResourceInfo(mountainPath, "dam:Asset"))),
        () -> assertFalse(disabled.canHandle(new ResourceInfo(mountainPath, "dam:Asset"))),
        () -> assertFalse(enabled.canHandle(new ResourceInfo(irrelevantPath, "cq:Page"))),
        () -> assertFalse(enabled.canHandle(new ResourceInfo(mountainContent, "nt:file")))
    );
  }

  @Test
  void canGetPublishData() {
    Map<String, Integer> assetPaths = Map.of(
        "/content/dam/core-components-examples/library/sample-assets/lava-rock-formation.jpg",
        DATA_SIZE
    );
    AssetPublicationHandler handler = context.registerInjectActivateService(
        AssetPublicationHandler.class, Map.of("enabled", true)
    );
    assetPaths.forEach(
        (assetPath, expectedSize) -> {
          PublishData<Asset> publishData = handler.getPublishData(assetPath);
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
