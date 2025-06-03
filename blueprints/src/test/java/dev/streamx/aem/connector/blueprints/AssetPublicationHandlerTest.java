package dev.streamx.aem.connector.blueprints;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import dev.streamx.aem.connector.test.util.OsgiConfigUtils;
import dev.streamx.blueprints.data.Asset;
import dev.streamx.sling.connector.PublishData;
import dev.streamx.sling.connector.ResourceInfo;
import dev.streamx.sling.connector.UnpublishData;
import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Objects;
import javax.jcr.Binary;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import org.apache.commons.io.IOUtils;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.engine.SlingRequestProcessor;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(AemContextExtension.class)
class AssetPublicationHandlerTest {

  private static final int DATA_SIZE = 3024;

  private final AemContext context = new AemContext(ResourceResolverType.JCR_OAK);

  @SuppressWarnings("resource")
  @BeforeEach
  void setup() throws PersistenceException {
    context.registerService(SlingRequestProcessor.class, mock(SlingRequestProcessor.class));
    context.load().json(
        "/dev/streamx/aem/connector/blueprints/sample-assets.json",
        "/content/dam/core-components-examples/library/sample-assets"
    );
    context.resourceResolver().delete(
        Objects.requireNonNull(context.resourceResolver().getResource(
            "/content/dam/core-components-examples/library/sample-assets/lava-rock-formation.jpg/jcr:content/renditions/original"
        ))
    );
    context.load().binaryFile(
        new ByteArrayInputStream("1".repeat(DATA_SIZE).getBytes()),
        "/content/dam/core-components-examples/library/sample-assets/lava-rock-formation.jpg/jcr:content/renditions/original"
    );
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
        AssetPublicationHandler.class, Map.of(
            "enabled", true,
            "jcr.prop.name.for.sx.type", "jcr:primaryType"
        )
    );
    ResourceInfo resourceInfo = new ResourceInfo(assetPath, "dam:Asset");
    assertThat(handler.canHandle(resourceInfo)).isTrue();
    PublishData<Asset> publishData = handler.getPublishData(assetPath);
    assertThat(publishData.getModel().getContent().array()).hasSize(DATA_SIZE);
    assertThat(publishData.getKey()).isEqualTo(assetPath);
    assertThat(publishData.getChannel()).isEqualTo("assets");
    assertThat(publishData.getModel()).isInstanceOf(Asset.class);
    assertThat(publishData.getProperties()).containsEntry("sx:type", "dam:Asset");

    UnpublishData<Asset> unpublishData = handler.getUnpublishData(assetPath);
    assertThat(unpublishData.getKey()).isEqualTo(assetPath);
    assertThat(unpublishData.getChannel()).isEqualTo("assets");

    OsgiConfigUtils.disableHandler(handler, context);
    assertThat(handler.canHandle(resourceInfo)).isFalse();
  }

  @Test
  void shouldRepublishOnlyChangedAssets() throws Exception {
    // given
    String resourcePath = "/content/dam/core-components-examples/library/sample-assets/lava-rock-formation.jpg";
    AssetPublicationHandler handler = context.registerInjectActivateService(AssetPublicationHandler.class);

    // when
    PublishData<Asset> publishData1 = handler.getPublishData(resourcePath);

    // then: expect the image to be published
    assertThat(publishData1).isNotNull();
    verifyStoredHash(resourcePath, "0a0d14967469e8679989086d22d60425a9991d4e3b2a7d3a1aa8615630990b6a");

    // when: publishing unchanged image
    PublishData<Asset> publishData2 = handler.getPublishData(resourcePath);

    // then: will not be published, since its hash has not changed
    assertThat(publishData2).isNull();
    verifyStoredHash(resourcePath, "0a0d14967469e8679989086d22d60425a9991d4e3b2a7d3a1aa8615630990b6a");

    // when: publishing edited image
    editImageInJcr(resourcePath);
    PublishData<Asset> publishData3 = handler.getPublishData(resourcePath);

    // then: expect the image to be published, since its hash has changed
    assertThat(publishData3).isNotNull();
    verifyStoredHash(resourcePath, "8902de85bac26f0eeab1c8f9a1adaeb8e4887d82f6a54107f7267be25b805e10");
  }

  @Test
  void shouldRepublishAssetAfterUnpublished() {
    // given
    String resourcePath = "/content/dam/core-components-examples/library/sample-assets/lava-rock-formation.jpg";
    AssetPublicationHandler handler = context.registerInjectActivateService(AssetPublicationHandler.class);

    // when
    PublishData<Asset> publishData1 = handler.getPublishData(resourcePath);

    // then: expect the image to be published
    assertThat(publishData1).isNotNull();
    verifyStoredHash(resourcePath, "0a0d14967469e8679989086d22d60425a9991d4e3b2a7d3a1aa8615630990b6a");

    // when: unpublishing the image
    UnpublishData<Asset> unpublishData = handler.getUnpublishData(resourcePath);

    // then: expecting also the hash to be removed
    assertThat(unpublishData).isNotNull();
    verifyHashNotStored(resourcePath);

    // when: republishing the same image
    PublishData<Asset> publishData2 = handler.getPublishData(resourcePath);

    // then: expect the image to be published, even if it has the same hash as when published before unpublishing
    assertThat(publishData2).isNotNull();
    verifyStoredHash(resourcePath, "0a0d14967469e8679989086d22d60425a9991d4e3b2a7d3a1aa8615630990b6a");
  }

  @SuppressWarnings("resource")
  private void verifyStoredHash(String resourcePath, String expected) {
    Resource hashResource = context.resourceResolver().resolve("/var/streamx/connector/sling/hashes/assets" + resourcePath);
    String hash = hashResource.getValueMap().get("lastPublishHash", String.class);
    assertThat(hash).isEqualTo(expected);
  }

  @SuppressWarnings("resource")
  private void verifyHashNotStored(String resourcePath) {
    Resource hashResource = context.resourceResolver().resolve("/var/streamx/connector/sling/hashes/assets" + resourcePath);
    assertThat(ResourceUtil.isNonExistingResource(hashResource)).isTrue();
  }

  @SuppressWarnings("resource")
  private void editImageInJcr(String resourcePath) throws RepositoryException, IOException {
    Session session = context.resourceResolver().adaptTo(Session.class);
    assertThat(session).isNotNull();

    Node binaryNode = session.getNode(resourcePath + "/jcr:content/renditions/original/jcr:content");
    Property binaryContentProperty = binaryNode.getProperty("jcr:data");

    Binary binaryContent = binaryContentProperty.getBinary();
    try (InputStream input = binaryContent.getStream()) {

      ByteArrayOutputStream output = new ByteArrayOutputStream();
      IOUtils.copy(input, output);

      // edit the image content by appending a byte
      output.write('a');

      // replace content
      Binary newBinaryContent = session.getValueFactory().createBinary(new ByteArrayInputStream(output.toByteArray()));
      binaryContentProperty.setValue(newBinaryContent);
    }

    session.save();
  }
}
