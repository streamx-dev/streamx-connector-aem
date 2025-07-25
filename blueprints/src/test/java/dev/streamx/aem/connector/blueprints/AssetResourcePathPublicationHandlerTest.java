package dev.streamx.aem.connector.blueprints;

import static org.assertj.core.api.Assertions.assertThat;

import dev.streamx.aem.connector.test.util.OsgiConfigUtils;
import dev.streamx.aem.connector.test.util.RandomBytesSlingRequestProcessor;
import dev.streamx.aem.connector.test.util.ResourceInfoFactory;
import dev.streamx.blueprints.data.Asset;
import dev.streamx.sling.connector.PublishData;
import dev.streamx.sling.connector.ResourceInfo;
import dev.streamx.sling.connector.StreamxPublicationException;
import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;
import java.util.Map;
import java.util.Map.Entry;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.engine.SlingRequestProcessor;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(AemContextExtension.class)
class AssetResourcePathPublicationHandlerTest {

  private final AemContext context = new AemContext(ResourceResolverType.JCR_OAK);

  private static final Map<String, Integer> assetPaths = Map.of(
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
      3024
  );

  private final SlingRequestProcessor requestProcessor = new RandomBytesSlingRequestProcessor(assetPaths);

  @BeforeEach
  void setup() {
    context.registerService(SlingRequestProcessor.class, requestProcessor);
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
    AssetResourcePathPublicationHandler enabled = context.registerInjectActivateService(
        AssetResourcePathPublicationHandler.class, Map.of("enabled", true)
    );
    AssetResourcePathPublicationHandler disabled = context.registerInjectActivateService(
        AssetResourcePathPublicationHandler.class, Map.of("enabled", false)
    );
    assertThat(resourceResolver.getResource(irrelevantPath)).isNotNull();
    assertThat(resourceResolver.getResource(mountainPath)).isNotNull();
    assertThat(resourceResolver.getResource(mountainContent)).isNotNull();
    assertThat(enabled.canHandle(ResourceInfoFactory.asset(mountainPath))).isFalse();
    assertThat(disabled.canHandle(ResourceInfoFactory.asset(mountainPath))).isFalse();
    assertThat(enabled.canHandle(ResourceInfoFactory.page(irrelevantPath))).isFalse();
    assertThat(enabled.canHandle(ResourceInfoFactory.file(mountainContent))).isFalse();
  }

  @Test
  void canGetPublishData() throws StreamxPublicationException {
    AssetResourcePathPublicationHandler handler = context.registerInjectActivateService(AssetResourcePathPublicationHandler.class);
    for (Entry<String, Integer> entry : assetPaths.entrySet()) {
      String assetPath = entry.getKey();
      Integer expectedSize = entry.getValue();
      ResourceInfo resourceInfo = ResourceInfoFactory.asset(assetPath);

      OsgiConfigUtils.enableHandler(handler, context);
      assertThat(handler.canHandle(resourceInfo)).isTrue();

      PublishData<Asset> publishData = handler.getPublishData(resourceInfo);
      assertThat(publishData.getModel().getContent().array()).hasSize(expectedSize);
      assertThat(publishData.getKey()).isEqualTo(assetPath);
      assertThat(publishData.getChannel()).isEqualTo("assets");
      assertThat(publishData.getModel()).isInstanceOf(Asset.class);
      assertThat(publishData.getProperties()).doesNotContainKey(BasePublicationHandler.SX_TYPE);

      OsgiConfigUtils.disableHandler(handler, context);
      assertThat(handler.canHandle(resourceInfo)).isFalse();
    }
  }

}
