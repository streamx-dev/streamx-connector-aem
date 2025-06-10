package dev.streamx.aem.connector.blueprints;

import static org.assertj.core.api.Assertions.assertThat;

import dev.streamx.aem.connector.test.util.OsgiConfigUtils;
import dev.streamx.aem.connector.test.util.RandomBytesSlingRequestProcessor;
import dev.streamx.blueprints.data.Renderer;
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
class RendererPublicationHandlerTest {

  private static final int TEXT_DATA_LENGTH = 888;
  private static final int BINARY_DATA_LENGTH = 1625;

  private final AemContext context = new AemContext(ResourceResolverType.JCR_OAK);

  private final SlingRequestProcessor requestProcessor = new RandomBytesSlingRequestProcessor(
      "/content/experience-fragments/templates/template.html",
      TEXT_DATA_LENGTH
  );

  @BeforeEach
  void setup() {
    context.registerService(SlingRequestProcessor.class, requestProcessor);
    context.registerInjectActivateService(PageDataService.class);
    context.load().json(
        "/dev/streamx/aem/connector/blueprints/sample-page-template.json",
        "/content/experience-fragments/templates/template"
    );
  }

  @SuppressWarnings("resource")
  @Test
  void mustHandle() {
    String pageTemplatePath = "/content/experience-fragments/templates/template";
    ResourceInfo pageTemplateResource = new ResourceInfo(pageTemplatePath, "cq:Page");
    String expectedKey = "/content/experience-fragments/templates/template.html";
    RendererPublicationHandler handler = context.registerInjectActivateService(
        RendererPublicationHandler.class
    );
    PublishData<Renderer> publishData = handler.getPublishData(pageTemplatePath);
    UnpublishData<Renderer> unpublishData = handler.getUnpublishData(pageTemplatePath);
    assertThat(context.resourceResolver().getResource(pageTemplatePath)).isNotNull();
    assertThat(handler.canHandle(pageTemplateResource)).isTrue();
    assertThat(publishData.getModel().getTemplate().array()).hasSize(BINARY_DATA_LENGTH);
    assertThat(publishData.getKey()).isEqualTo(expectedKey);
    assertThat(publishData.getProperties()).doesNotContainKey(BasePublicationHandler.SX_TYPE);
    assertThat(unpublishData.getKey()).isEqualTo(expectedKey);

    OsgiConfigUtils.disableHandler(handler, context);
    assertThat(handler.canHandle(pageTemplateResource)).isFalse();
  }
}
