package dev.streamx.aem.connector.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import dev.streamx.sling.connector.ResourceInfo;
import java.util.List;
import java.util.Map;
import org.apache.sling.testing.mock.osgi.MockOsgi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.osgi.service.event.Event;

class AemDeletionEventHandlerTest extends BaseAemEventHandlerTest {

  private AemDeletionEventHandler handler;

  @BeforeEach
  void setup() {
    handler = spy(context.registerInjectActivateService(
        AemDeletionEventHandler.class,
        Map.of("resource.properties.to.load", new String[]{"jcr:primaryType"})
    ));
  }

  @Test
  void shouldHandleEvents() throws Exception {
    // when
    handler.handleEvent(
        createDeleteEventForRegisteredPageResource("preDelete", "/content/we-retail/us/en"));
    handler.handleEvent(
        createDeleteEventForRegisteredPageResource("preDelete", "/content/we-purchase/us/en"));
    handler.handleEvent(
        createDeleteEventForRegisteredPageResource("postDelete", "/content/we-sell/us/en"));

    // then
    verifyUnpublishedResources(2, Map.of(
        "/content/we-retail/us/en", "cq:Page",
        "/content/we-purchase/us/en", "cq:Page"
    ));

    // and
    verifyNoPublishedResources();
  }

  @Test
  void shouldSkipHandlingEventIfPublicationServiceIsDisabled() throws Exception {
    // given
    disableStreamxPublicationService();

    // when
    handler.handleEvent(
        createDeleteEventForRegisteredPageResource("preDelete", "/content/we-retail/us/en"));

    // then
    verify(handler, never()).createResourceResolver();
    verifyNoPublishedResources();
    verifyNoUnpublishedResources();
  }

  @Test
  void shouldParseNonStringProperties() throws Exception {
    handler = spy(context.registerInjectActivateService(
        AemDeletionEventHandler.class,
        Map.of("resource.properties.to.load", new String[]{
            "stringProp", "intProp", "boolProp", "multiStringProp", "nullProp", "jcr:primaryType"
        })
    ));

    String resourcePath = "/content/we-retail/us/en";
    String resourceJson =
        "{" +
        "  \"stringProp\": \"abc\"," +
        "  \"intProp\": 123," +
        "  \"boolProp\": true," +
        "  \"multiStringProp\": [\"def\", \"ghi\", \"jkl\"]," +
        "  \"nullProp\": null," +
        "  \"jcr:primaryType\": \"cq:Page\"" +
        "}";
    registerCustomResource(resourcePath, resourceJson);

    Event event = createDeleteEvent("preDelete", resourcePath);
    handler.handleEvent(event);

    // then
    List<ResourceInfo> unpublishedResources = verifyUnpublishedResources(
        1, Map.of(resourcePath, "cq:Page")
    );

    assertThat(unpublishedResources.get(0).getProperties())
        .hasSize(6)
        .containsEntry("stringProp", "abc")
        .containsEntry("intProp", "123")
        .containsEntry("boolProp", "true")
        .containsEntry("multiStringProp", "def")
        .containsEntry("nullProp", null)
        .containsEntry("jcr:primaryType", "cq:Page");

    // and
    verifyNoPublishedResources();
  }

  @Test
  void shouldAdjustToModifiedConfiguration() throws ReflectiveOperationException {
    // given
    assertResourcePropertiesToLoad(handler, "jcr:primaryType");

    // when
    MockOsgi.modified(handler, context.bundleContext(), Map.of("resource.properties.to.load", new String[]{"foobar"}));

    // then
    assertResourcePropertiesToLoad(handler, "foobar");
  }

  private Event createDeleteEventForRegisteredPageResource(String type, String pageResourcePath) {
    registerResource(pageResourcePath, "cq:Page");
    return createDeleteEvent(type, pageResourcePath);
  }

  private static Event createDeleteEvent(String type, String pageResourcePath) {
    return new Event(
        "com/adobe/cq/resource/delete",
        Map.of(
            "type", type,
            "path", pageResourcePath,
            "userId", "admin"
        )
    );
  }
}