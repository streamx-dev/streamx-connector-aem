package dev.streamx.aem.connector.blueprints;

import static org.assertj.core.api.Assertions.assertThat;

import dev.streamx.aem.connector.test.util.OsgiConfigUtils;
import dev.streamx.aem.connector.test.util.RandomBytesSlingRequestProcessor;
import dev.streamx.blueprints.data.Page;
import dev.streamx.sling.connector.PublishData;
import dev.streamx.sling.connector.ResourceInfo;
import dev.streamx.sling.connector.UnpublishData;
import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;
import java.util.Map;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import org.apache.sling.engine.SlingRequestProcessor;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(AemContextExtension.class)
class PagePublicationHandlerTest {

  private static final int TEXT_DATA_LENGTH = 888;
  private static final int BINARY_DATA_LENGTH = 1625;

  private final AemContext context = new AemContext(ResourceResolverType.JCR_OAK);

  private final SlingRequestProcessor requestProcessor = new RandomBytesSlingRequestProcessor(
      "/content/pages/usual-aem-page.html",
      TEXT_DATA_LENGTH
  );

  @BeforeEach
  void setup() {
    context.registerService(SlingRequestProcessor.class, requestProcessor);
    context.registerInjectActivateService(PageDataService.class);
    context.load().json(
        "/dev/streamx/aem/connector/blueprints/usual-aem-page.json",
        "/content/pages/usual-aem-page"
    );
  }

  @SuppressWarnings("resource")
  @Test
  void mustHandle() throws RepositoryException {
    String pagePath = "/content/pages/usual-aem-page";
    Node pageNode = context.resourceResolver().getResource(pagePath).adaptTo(Node.class);
    ResourceInfo resourceInfo = new ResourceInfo(pagePath, Map.of(
        "jcr:primaryType", pageNode.getProperty("jcr:primaryType").getString(),
        "jcr:content/cq:template", pageNode.getProperty("jcr:content/cq:template").getString()
    ));

    String expectedKey = "/content/pages/usual-aem-page.html";
    PagePublicationHandler handler = context.registerInjectActivateService(PagePublicationHandler.class);
    PublishData<Page> publishData = handler.getPublishData(resourceInfo);
    UnpublishData<Page> unpublishData = handler.getUnpublishData(resourceInfo);
    assertThat(handler.canHandle(resourceInfo)).isTrue();
    assertThat(publishData.getModel().getContent().array()).hasSize(BINARY_DATA_LENGTH);
    assertThat(publishData.getKey()).isEqualTo(expectedKey);
    assertThat(publishData.getProperties()).containsEntry(BasePublicationHandler.SX_TYPE, "/conf/firsthops/settings/wcm/templates/page-content");
    assertThat(unpublishData.getKey()).isEqualTo(expectedKey);
    assertThat(unpublishData.getProperties()).containsEntry(BasePublicationHandler.SX_TYPE, "/conf/firsthops/settings/wcm/templates/page-content");

    OsgiConfigUtils.disableHandler(handler, context);
    assertThat(handler.canHandle(resourceInfo)).isFalse();
  }

  @SuppressWarnings("resource")
  @Test
  void shouldNotFailWhenPropertiesForLoadingSxTypeAreNotPresentInReceivedResourceInfoObject() throws RepositoryException {
    String pagePath = "/content/pages/usual-aem-page";
    Node pageNode = context.resourceResolver().getResource(pagePath).adaptTo(Node.class);
    ResourceInfo resourceInfo = new ResourceInfo(pagePath, Map.of(
        "jcr:primaryType", pageNode.getProperty("jcr:primaryType").getString()
    ));

    PagePublicationHandler handler = context.registerInjectActivateService(PagePublicationHandler.class);
    assertThat(handler.canHandle(resourceInfo)).isTrue();

    PublishData<Page> publishData = handler.getPublishData(resourceInfo);
    assertThat(publishData.getProperties()).doesNotContainKey(BasePublicationHandler.SX_TYPE);

    UnpublishData<Page> unpublishData = handler.getUnpublishData(resourceInfo);
    assertThat(unpublishData.getProperties()).doesNotContainKey(BasePublicationHandler.SX_TYPE);
  }
}
