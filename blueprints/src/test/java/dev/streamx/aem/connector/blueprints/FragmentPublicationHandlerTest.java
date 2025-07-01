package dev.streamx.aem.connector.blueprints;

import static org.assertj.core.api.Assertions.assertThat;

import dev.streamx.aem.connector.test.util.OsgiConfigUtils;
import dev.streamx.aem.connector.test.util.RandomBytesSlingRequestProcessor;
import dev.streamx.aem.connector.test.util.ResourceInfoFactory;
import dev.streamx.blueprints.data.Fragment;
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
class FragmentPublicationHandlerTest {

  private static final int TEXT_DATA_LENGTH = 888;
  private static final int BINARY_DATA_LENGTH = 1625;

  private final AemContext context = new AemContext(ResourceResolverType.JCR_OAK);

  private final SlingRequestProcessor requestProcessor = new RandomBytesSlingRequestProcessor(
      "/content/experience-fragments/fragment.html", TEXT_DATA_LENGTH
  );

  @BeforeEach
  void setup() {
    context.registerService(SlingRequestProcessor.class, requestProcessor);
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
  void mustHandle() throws RepositoryException {
    String fragmentPath = "/content/experience-fragments/fragment";
    Node fragmentNode = context.resourceResolver().getResource(fragmentPath).adaptTo(Node.class);
    ResourceInfo fragmentResourceInfo = new ResourceInfo(fragmentPath, Map.of(
        "jcr:primaryType", fragmentNode.getProperty("jcr:primaryType").getString(),
        "jcr:content/cq:template", fragmentNode.getProperty("jcr:content/cq:template").getString()
    ));

    String expectedKey = "/content/experience-fragments/fragment.html";
    FragmentPublicationHandler handler = context.registerInjectActivateService(FragmentPublicationHandler.class);
    PublishData<Fragment> publishData = handler.getPublishData(fragmentResourceInfo);
    UnpublishData<Fragment> unpublishData = handler.getUnpublishData(fragmentResourceInfo);
    assertThat(handler.canHandle(fragmentResourceInfo)).isTrue();
    assertThat(publishData.getModel().getContent().array()).hasSize(BINARY_DATA_LENGTH);
    assertThat(publishData.getKey()).isEqualTo(expectedKey);
    assertThat(publishData.getProperties()).containsEntry(BasePublicationHandler.SX_TYPE, "/conf/firsthops/settings/wcm/templates/page-content");
    assertThat(unpublishData.getKey()).isEqualTo(expectedKey);
    assertThat(unpublishData.getProperties()).containsEntry(BasePublicationHandler.SX_TYPE, "/conf/firsthops/settings/wcm/templates/page-content");

    OsgiConfigUtils.disableHandler(handler, context);
    assertThat(handler.canHandle(fragmentResourceInfo)).isFalse();
  }

  @Test
  void shouldNotHandleUsualPage() {
    String pagePath = "/content/usual-aem-page";
    ResourceInfo pageResourceInfo = ResourceInfoFactory.create(pagePath, "cq:Page");

    FragmentPublicationHandler handler = context.registerInjectActivateService(FragmentPublicationHandler.class);
    assertThat(handler.canHandle(pageResourceInfo)).isFalse();
  }
}
