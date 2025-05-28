package dev.streamx.aem.connector.blueprints;

import static org.assertj.core.api.Assertions.assertThat;

import dev.streamx.aem.connector.test.util.OsgiConfigUtils;
import dev.streamx.aem.connector.test.util.RandomBytesWriter;
import dev.streamx.blueprints.data.Renderer;
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
class RendererPublicationHandlerTest {

  private static final int TEXT_DATA_LENGTH = 888;
  private static final int BINARY_DATA_LENGTH = 1625;

  private final AemContext context = new AemContext(ResourceResolverType.JCR_OAK);

  private final SlingRequestProcessor basicRequestProcessor = (HttpServletRequest request, HttpServletResponse response, ResourceResolver resolver) -> {
    String requestURI = request.getRequestURI();
    if (requestURI.equals("/content/experience-fragments/templates/template.html")) {
      RandomBytesWriter.writeRandomBytes(response, TEXT_DATA_LENGTH);
    } else {
      response.getWriter().write("<html><body><h1>Not Found</h1></body></html>");
    }
  };

  @BeforeEach
  void setup() {
    context.registerService(SlingRequestProcessor.class, basicRequestProcessor);
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
    assertThat(unpublishData.getKey()).isEqualTo(expectedKey);

    OsgiConfigUtils.disableHandler(handler, context);
    assertThat(handler.canHandle(pageTemplateResource)).isFalse();
  }
}
