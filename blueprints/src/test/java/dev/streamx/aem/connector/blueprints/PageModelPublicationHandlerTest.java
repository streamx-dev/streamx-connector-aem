package dev.streamx.aem.connector.blueprints;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import dev.streamx.aem.connector.test.util.FixedResponseSlingRegexRequestProcessor;
import dev.streamx.aem.connector.test.util.OsgiConfigUtils;
import dev.streamx.aem.connector.test.util.ResourceInfoFactory;
import dev.streamx.blueprints.data.Data;
import dev.streamx.sling.connector.PublishData;
import dev.streamx.sling.connector.ResourceInfo;
import dev.streamx.sling.connector.UnpublishData;
import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;
import java.util.HashMap;
import java.util.Map;
import javax.jcr.Node;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.engine.SlingRequestProcessor;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(AemContextExtension.class)
class PageModelPublicationHandlerTest {

  private static final String PAGE_RESOURCE_PATH = "/content/pages/usual-aem-page";
  private static final String PAGE_MODEL_JSON = "{'id': '1', 'title': 'API page', 'items': { 'item1': {} }'";

  private final AemContext context = new AemContext(ResourceResolverType.JCR_OAK);

  private final SlingRequestProcessor requestProcessor = spy(new FixedResponseSlingRegexRequestProcessor(
      PAGE_RESOURCE_PATH + ".*",
      PAGE_MODEL_JSON
  ));

  @BeforeEach
  void setup() {
    context.registerService(SlingRequestProcessor.class, requestProcessor);
    context.registerInjectActivateService(PageDataService.class);
    context.load().json(
        "src/test/resources/dev/streamx/aem/connector/blueprints/usual-aem-page.json",
        PAGE_RESOURCE_PATH
    );
  }

  @Test
  void shouldHandlePageModelPublication() throws Exception {
    PageModelPublicationHandler handler = registerHandler(
        "enabled", true,
        "page.resource.path.regex", "^/content/pages/.*"
    );

    assertPageIsIngestedWithKeySuffix(handler, ".model.json");
  }

  @Test
  void shouldHandlePageModelPublicationWithAnySelectorsAndExtensionConfiguration() throws Exception {
    PageModelPublicationHandler handler = registerHandler(
        "enabled", true,
        "page.resource.path.regex", "^/content/pages/.*",
        "selectors.to.append", new String[]{"abc", "def", "ghi"},
        "extension.to.append", "txt"
    );

    assertPageIsIngestedWithKeySuffix(handler, ".abc.def.ghi.txt");
  }

  @Test
  void shouldHandlePageModelPublicationWithNullSelectorsAndExtensionInConfiguration() throws Exception {
    PageModelPublicationHandler handler = registerHandler(
        "enabled", true,
        "page.resource.path.regex", "^/content/pages/.*",
        "selectors.to.append", null,
        "extension.to.append", null
    );

    assertPageIsIngestedWithKeySuffix(handler, StringUtils.EMPTY);
  }

  @SuppressWarnings({"resource", "ConstantConditions"})
  private void assertPageIsIngestedWithKeySuffix(PageModelPublicationHandler handler, String expectedKeySuffix) throws Exception {
    Node pageNode = context.resourceResolver().getResource(PAGE_RESOURCE_PATH).adaptTo(Node.class);
    ResourceInfo PAGE_RESOURCE = new ResourceInfo(PAGE_RESOURCE_PATH, Map.of(
        "jcr:primaryType", pageNode.getProperty("jcr:primaryType").getString(),
        "jcr:content/cq:template", pageNode.getProperty("jcr:content/cq:template").getString()
    ));

    assertThat(handler.canHandle(PAGE_RESOURCE)).isTrue();

    PublishData<Data> publishData = handler.getPublishData(PAGE_RESOURCE);
    assertThat(publishData).isNotNull();
    assertThat(publishData.getKey()).isEqualTo(PAGE_RESOURCE_PATH + expectedKeySuffix);
    assertThat(publishData.getModel().getContent().array()).asString().isEqualTo(PAGE_MODEL_JSON);
    assertThat(publishData.getProperties()).containsEntry(BasePublicationHandler.SX_TYPE, "/conf/firsthops/settings/wcm/templates/page-content");

    UnpublishData<Data> unpublishData = handler.getUnpublishData(PAGE_RESOURCE);
    assertThat(unpublishData.getKey()).isEqualTo(PAGE_RESOURCE_PATH + expectedKeySuffix);
    assertThat(unpublishData.getProperties()).containsEntry(BasePublicationHandler.SX_TYPE, "/conf/firsthops/settings/wcm/templates/page-content");

    // verify internal request was sent with the same url suffix, and that it was called only once (not for getUnpublishData)
    verify(requestProcessor).processRequest(
        argThat(request ->
            request.getRequestURI().equals(PAGE_RESOURCE_PATH + expectedKeySuffix)
            && request.getParameter("resolveStreamxDirectives").equals("true")),
        any(),
        any()
    );

    OsgiConfigUtils.disableHandler(handler, context);
    assertThat(handler.canHandle(PAGE_RESOURCE)).isFalse();
  }

  @Test
  void shouldNotHandlePageModelPublicationWhenPageResourcePathDoesNotMatchTheConfiguredRegex() {
    ResourceInfo resource = ResourceInfoFactory.create(PAGE_RESOURCE_PATH, "cq:Page");

    PageModelPublicationHandler handler = registerHandler(
        "enabled", true,
        "page.resource.path.regex", "/foo" + PAGE_RESOURCE_PATH
    );

    assertThat(handler.canHandle(resource)).isFalse();
  }

  @Test
  void shouldNotHandleExperienceFragments() {
    ResourceInfo resource = ResourceInfoFactory.create("/content/experience-fragments/fragment-1", "cq:Page");

    PageModelPublicationHandler handler = registerHandler("enabled", true);

    assertThat(handler.canHandle(resource)).isFalse();
  }

  @Test
  void shouldNotHandleNonPages() {
    ResourceInfo resource = ResourceInfoFactory.create("/data/file-1", "dam:Asset");

    PageModelPublicationHandler handler = registerHandler(
        "enabled", true,
        "page.resource.path.regex", "/data/file-1"
    );

    assertThat(handler.canHandle(resource)).isFalse();
  }

  @Test
  void shouldBeDisabledByDefault() {
    ResourceInfo resource = ResourceInfoFactory.create(PAGE_RESOURCE_PATH, "cq:Page");

    PageModelPublicationHandler handler = registerHandler();

    assertThat(handler.canHandle(resource)).isFalse();
  }

  private PageModelPublicationHandler registerHandler(Object... properties) {
    assertThat(properties.length).isEven();

    Map<String, Object> propertyMap = new HashMap<>();
    for (int i = 0; i < properties.length; i += 2) {
      propertyMap.put((String) properties[i], properties[i + 1]);
    }

    return context.registerInjectActivateService(
        PageModelPublicationHandler.class,
        propertyMap
    );
  }
}
