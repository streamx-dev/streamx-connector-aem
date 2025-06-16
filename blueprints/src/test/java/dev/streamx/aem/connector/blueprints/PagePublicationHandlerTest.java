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
  void mustHandle() {
    String pagePath = "/content/pages/usual-aem-page";
    ResourceInfo pageResource = new ResourceInfo(pagePath, "cq:Page");
    String expectedKey = "/content/pages/usual-aem-page.html";
    PagePublicationHandler handler = context.registerInjectActivateService(PagePublicationHandler.class);
    PublishData<Page> publishData = handler.getPublishData(pagePath);
    UnpublishData<Page> unpublishData = handler.getUnpublishData(pagePath);
    assertThat(context.resourceResolver().getResource(pagePath)).isNotNull();
    assertThat(handler.canHandle(pageResource)).isTrue();
    assertThat(publishData.getModel().getContent().array()).hasSize(BINARY_DATA_LENGTH);
    assertThat(publishData.getKey()).isEqualTo(expectedKey);
    assertThat(publishData.getProperties()).containsEntry(BasePublicationHandler.SX_TYPE, "/conf/firsthops/settings/wcm/templates/page-content");
    assertThat(unpublishData.getKey()).isEqualTo(expectedKey);

    OsgiConfigUtils.disableHandler(handler, context);
    assertThat(handler.canHandle(pageResource)).isFalse();
  }
}
