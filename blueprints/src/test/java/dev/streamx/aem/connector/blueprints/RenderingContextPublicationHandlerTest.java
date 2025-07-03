package dev.streamx.aem.connector.blueprints;

import static org.assertj.core.api.Assertions.assertThat;

import dev.streamx.aem.connector.test.util.OsgiConfigUtils;
import dev.streamx.aem.connector.test.util.RandomBytesSlingRequestProcessor;
import dev.streamx.aem.connector.test.util.ResourceInfoFactory;
import dev.streamx.blueprints.data.RenderingContext;
import dev.streamx.blueprints.data.RenderingContext.OutputFormat;
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
class RenderingContextPublicationHandlerTest {

  private static final int DATA_SIZE = 888;

  private final AemContext context = new AemContext(ResourceResolverType.JCR_OAK);

  private final SlingRequestProcessor requestProcessor = new RandomBytesSlingRequestProcessor(
      "/content/experience-fragments/templates/sample-rendering-context.html",
      DATA_SIZE
  );

  @BeforeEach
  void setup() {
    context.registerService(SlingRequestProcessor.class, requestProcessor);
    context.registerInjectActivateService(PageDataService.class);
    context.load().json(
        "/dev/streamx/aem/connector/blueprints/sample-rendering-context.json",
        "/content/experience-fragments/templates/sample-rendering-context"
    );
  }

  @SuppressWarnings("resource")
  @Test
  void mustHandle() {
    String resourcePath = "/content/experience-fragments/templates/sample-rendering-context";
    ResourceInfo resource = ResourceInfoFactory.page(resourcePath);
    String expectedKey = "/content/experience-fragments/templates/sample-rendering-context";
    RenderingContextPublicationHandler handler = context.registerInjectActivateService(
        RenderingContextPublicationHandler.class
    );
    assertThat(handler.canHandle(resource)).isTrue();

    PublishData<RenderingContext> publishData = handler.getPublishData(resource);
    assertThat(publishData.getKey()).isEqualTo(expectedKey);
    RenderingContext model = publishData.getModel();
    assertThat(model.getRendererKey()).isEqualTo(expectedKey);
    assertThat(model.getDataKeyMatchPattern()).isEqualTo("data.*");
    assertThat(model.getDataTypeMatchPattern()).isEqualTo("test-type/.*");
    assertThat(model.getOutputKeyTemplate()).isEqualTo("key-1");
    assertThat(model.getOutputTypeTemplate()).isEqualTo("data-type3-output-type-pattern-{{id}}");
    assertThat(model.getOutputFormat()).isSameAs(OutputFormat.PAGE);
    assertThat(publishData.getProperties()).doesNotContainKey(BasePublicationHandler.SX_TYPE);

    UnpublishData<RenderingContext> unpublishData = handler.getUnpublishData(resource);
    assertThat(context.resourceResolver().getResource(resourcePath)).isNotNull();
    assertThat(unpublishData.getKey()).isEqualTo(expectedKey);
    assertThat(unpublishData.getProperties()).doesNotContainKey(BasePublicationHandler.SX_TYPE);

    OsgiConfigUtils.disableHandler(handler, context);
    assertThat(handler.canHandle(resource)).isFalse();
  }
}
