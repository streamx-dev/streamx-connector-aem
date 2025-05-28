package dev.streamx.aem.connector.blueprints;

import static org.assertj.core.api.Assertions.assertThat;

import dev.streamx.aem.connector.test.util.OsgiConfigUtils;
import dev.streamx.aem.connector.test.util.RandomBytesWriter;
import dev.streamx.blueprints.data.RenderingContext;
import dev.streamx.blueprints.data.RenderingContext.OutputType;
import dev.streamx.sling.connector.PublishData;
import dev.streamx.sling.connector.ResourceInfo;
import dev.streamx.sling.connector.UnpublishData;
import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.engine.SlingRequestProcessor;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(AemContextExtension.class)
class RenderingContextPublicationHandlerTest {

  private static final int DATA_SIZE = 888;

  private final AemContext context = new AemContext(ResourceResolverType.JCR_OAK);

  private final SlingRequestProcessor basicRequestProcessor = (HttpServletRequest request, HttpServletResponse response, ResourceResolver resolver) -> {
    String requestURI = request.getRequestURI();
    if (requestURI.equals(
        "/content/experience-fragments/templates/sample-rendering-context.html")) {
      RandomBytesWriter.writeRandomBytes(response, DATA_SIZE);
    } else {
      response.getWriter().write("<html><body><h1>Not Found</h1></body></html>");
    }
  };

  @BeforeEach
  void setup() {
    context.registerService(SlingRequestProcessor.class, basicRequestProcessor);
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
    ResourceInfo resource = new ResourceInfo(resourcePath, "cq:Page");
    String expectedKey = "/content/experience-fragments/templates/sample-rendering-context";
    RenderingContextPublicationHandler handler = context.registerInjectActivateService(
        RenderingContextPublicationHandler.class
    );
    PublishData<RenderingContext> publishData = handler.getPublishData(resourcePath);
    assertThat(handler.canHandle(resource)).isTrue();
    RenderingContext model = publishData.getModel();
    assertThat(model.getDataKeyMatchPattern()).isEqualTo("data.*");
    assertThat(model.getRendererKey()).isEqualTo(expectedKey);
    assertThat(model.getOutputKeyTemplate()).isEqualTo("key-1");
    assertThat(model.getOutputType()).isSameAs(OutputType.PAGE);
    UnpublishData<RenderingContext> unpublishData = handler.getUnpublishData(resourcePath);
    assertThat(context.resourceResolver().getResource(resourcePath)).isNotNull();
    assertThat(publishData.getKey()).isEqualTo(expectedKey);
    assertThat(unpublishData.getKey()).isEqualTo(expectedKey);

    OsgiConfigUtils.disableHandler(handler, context);
    assertThat(handler.canHandle(resource)).isFalse();
  }
}
