package dev.streamx.aem.connector.impl;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import dev.streamx.aem.connector.test.util.ResourceResolverFactoryMocks;
import java.util.Map;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.testing.mock.osgi.MockOsgi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.osgi.framework.Constants;
import org.osgi.service.event.Event;

class AemDeletionEventHandlerTest extends BaseAemEventHandlerTest {

  private AemDeletionEventHandler handler;

  @BeforeEach
  void setup() throws Exception {
    context.registerService(
        ResourceResolverFactory.class,
        ResourceResolverFactoryMocks.withFixedResourcePrimaryNodeType("cq:Page", context),
        Constants.SERVICE_RANKING, Integer.MAX_VALUE
    );

    handler = spy(context.registerInjectActivateService(
        AemDeletionEventHandler.class,
        Map.of("resource.properties.to.load", new String[]{"jcr:primaryType"})
    ));
  }

  @Test
  void shouldHandleEvents() throws Exception {
    // when
    handler.handleEvent(createDeleteEvent("preDelete", "http://localhost:4502/content/we-retail/us/en"));
    handler.handleEvent(createDeleteEvent("preDelete", "http://localhost:4502/content/we-purchase/us/en"));
    handler.handleEvent(createDeleteEvent("postDelete", "http://localhost:4502/content/we-sell/us/en"));

    // then
    verifyUnpublishedResources(2, Map.of(
        "http://localhost:4502/content/we-retail/us/en", "cq:Page",
        "http://localhost:4502/content/we-purchase/us/en", "cq:Page"
    ));

    // and
    verifyNoPublishedResources();
  }

  @Test
  void shouldSkipHandlingEventIfPublicationServiceIsDisabled() throws Exception {
    // given
    disableStreamxPublicationService();

    // when
    handler.handleEvent(createDeleteEvent("preDelete", "http://localhost:4502/content/we-retail/us/en"));

    // then
    verify(handler, never()).createResourceResolver();
    verifyNoPublishedResources();
    verifyNoUnpublishedResources();
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

  private static Event createDeleteEvent(String type, String resourcePath) {
    return new Event(
        "com/adobe/cq/resource/delete",
        Map.of(
            "type", type,
            "path", resourcePath,
            "userId", "admin"
        )
    );
  }
}