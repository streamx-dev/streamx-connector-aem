package dev.streamx.aem.connector.blueprints;

import static org.assertj.core.api.Assertions.assertThat;

import dev.streamx.aem.connector.test.util.OsgiConfigUtils;
import dev.streamx.aem.connector.test.util.RandomBytesWriter;
import dev.streamx.blueprints.data.Fragment;
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
class FragmentPublicationHandlerTest {

  private static final int TEXT_DATA_LENGTH = 888;
  private static final int BINARY_DATA_LENGTH = 1625;

  private final AemContext context = new AemContext(ResourceResolverType.JCR_OAK);

  private final SlingRequestProcessor basicRequestProcessor = (HttpServletRequest request, HttpServletResponse response, ResourceResolver resolver) -> {
    String requestURI = request.getRequestURI();
    if (requestURI.equals("/content/experience-fragments/fragment.html")) {
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
        "/dev/streamx/aem/connector/blueprints/franklin-page.json",
        "/content/franklin-page"
    );
    context.load().json(
        "/dev/streamx/aem/connector/blueprints/usual-aem-page.json",
        "/content/usual-aem-page"
    );
    context.load().json(
        "/dev/streamx/aem/connector/blueprints/usual-aem-page.json",
        "/content/experience-fragments/fragment"
    );
    context.build().resource("/content/random-page").commit();
  }

  @SuppressWarnings("resource")
  @Test
  void mustHandle() {
    String pagePath = "/content/usual-aem-page";
    ResourceInfo pageResource = new ResourceInfo(pagePath, "cq:Page");
    String fragmentPath = "/content/experience-fragments/fragment";
    ResourceInfo fragmentResource = new ResourceInfo(fragmentPath, "cq:Page");
    String expectedKey = "/content/experience-fragments/fragment.html";
    FragmentPublicationHandler handler = context.registerInjectActivateService(
        FragmentPublicationHandler.class
    );
    PublishData<Fragment> publishData = handler.getPublishData(fragmentPath);
    UnpublishData<Fragment> unpublishData = handler.getUnpublishData(fragmentPath);
    assertThat(context.resourceResolver().getResource(pagePath)).isNotNull();
    assertThat(handler.canHandle(pageResource)).isFalse();
    assertThat(handler.canHandle(fragmentResource)).isTrue();
    assertThat(publishData.getModel().getContent().array()).hasSize(BINARY_DATA_LENGTH);
    assertThat(publishData.getKey()).isEqualTo(expectedKey);
    assertThat(publishData.getProperties()).containsEntry("sx:type", "/conf/firsthops/settings/wcm/templates/page-content");
    assertThat(unpublishData.getKey()).isEqualTo(expectedKey);

    OsgiConfigUtils.disableHandler(handler, context);
    assertThat(handler.canHandle(fragmentResource)).isFalse();
  }
}
