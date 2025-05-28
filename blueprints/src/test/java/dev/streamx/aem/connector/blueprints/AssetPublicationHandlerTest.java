package dev.streamx.aem.connector.blueprints;

import static org.assertj.core.api.Assertions.assertThat;

import dev.streamx.blueprints.data.Asset;
import dev.streamx.sling.connector.PublishData;
import dev.streamx.sling.connector.ResourceInfo;
import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;
import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
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

  private final SlingRequestProcessor basicRequestProcessor = (HttpServletRequest request, HttpServletResponse response, ResourceResolver resourceResolver) -> {
    response.setContentType("text/html");
    response.getWriter().write("<html><body><h1>Not Found</h1></body></html>");
  };

  @SuppressWarnings("resource")
  @BeforeEach
  void setup() throws PersistenceException {
    context.registerService(SlingRequestProcessor.class, basicRequestProcessor);
    context.load().json(
        "/dev/streamx/aem/connector/blueprints/sample-assets.json",
        "/content/dam/core-components-examples/library/sample-assets"
    );
    context.resourceResolver().delete(
        Objects.requireNonNull(context.resourceResolver().getResource(
            "/content/dam/core-components-examples/library/sample-assets/lava-rock-formation.jpg/jcr:content/renditions/original"
        ))
    );
    byte[] bytes = new byte[DATA_SIZE];
    Arrays.fill(bytes, NumberUtils.BYTE_ONE);
    context.load().binaryFile(
        new ByteArrayInputStream(bytes),
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
    assertThat(resourceResolver.getResource(irrelevantPath)).isNotNull();
    assertThat(resourceResolver.getResource(mountainPath)).isNotNull();
    assertThat(resourceResolver.getResource(mountainContent)).isNotNull();
    assertThat(enabled.canHandle(new ResourceInfo(mountainPath, "dam:Asset"))).isTrue();
    assertThat(disabled.canHandle(new ResourceInfo(mountainPath, "dam:Asset"))).isFalse();
    assertThat(enabled.canHandle(new ResourceInfo(irrelevantPath, "cq:Page"))).isFalse();
    assertThat(enabled.canHandle(new ResourceInfo(mountainContent, "nt:file"))).isFalse();
  }

  @Test
  void canGetPublishData() {
    String assetPath = "/content/dam/core-components-examples/library/sample-assets/lava-rock-formation.jpg";
    AssetPublicationHandler handler = context.registerInjectActivateService(
        AssetPublicationHandler.class, Map.of("enabled", true)
    );
    PublishData<Asset> publishData = handler.getPublishData(assetPath);
    assertThat(publishData.getModel().getContent().array()).hasSize(DATA_SIZE);
    assertThat(publishData.getKey()).isEqualTo(assetPath);
    assertThat(publishData.getChannel()).isEqualTo("assets");
    assertThat(publishData.getModel()).isInstanceOf(Asset.class);
  }
}
