package dev.streamx.aem.connector.impl;

import com.day.cq.replication.ReplicationAction;
import com.day.cq.replication.ReplicationActionType;
import dev.streamx.aem.connector.test.util.ResourceResolverFactoryMocks;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;
import java.util.Map;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.testing.mock.osgi.MockOsgi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.osgi.framework.Constants;
import org.osgi.service.event.Event;

@ExtendWith(AemContextExtension.class)
class AemReplicationEventHandlerTest extends BaseAemEventHandlerTest {

  private AemReplicationEventHandler handler;

  @BeforeEach
  void setup() throws Exception {
    context.registerService(
        ResourceResolverFactory.class,
        ResourceResolverFactoryMocks.withFixedResourcePrimaryNodeType("dam:Asset", context),
        Constants.SERVICE_RANKING, Integer.MAX_VALUE
    );

    handler = context.registerInjectActivateService(
        AemReplicationEventHandler.class,
        Map.of("properties.to.load.from.jcr", new String[]{"jcr:primaryType"})
    );
  }

  @Test
  void shouldHandleEvents() throws Exception {
    // given
    Event activate = new Event(
        ReplicationAction.EVENT_TOPIC,
        Map.of(
            "type", ReplicationActionType.ACTIVATE.getName(),
            "paths", new String[]{
                "http://localhost:4502/content/we-retail/us/en",
                "/content/wknd/us/en"
            },
            "userId", "admin"
        )
    );
    Event deactivate = new Event(
        ReplicationAction.EVENT_TOPIC,
        Map.of(
            "type", ReplicationActionType.DEACTIVATE.getName(),
            "paths", new String[]{
                "http://localhost:4502/content/we-retail/us/pl",
                "/content/wknd/us/pl"
            },
            "userId", "admin"
        )
    );
    Event delete = new Event(
        ReplicationAction.EVENT_TOPIC,
        Map.of(
            "type", ReplicationActionType.DELETE.getName(),
            "paths", new String[]{
                "http://localhost:4502/content/we-retail/us/fr",
                "/content/wknd/us/fr"
            },
            "userId", "admin"
        )
    );

    // when
    handler.handleEvent(activate);
    handler.handleEvent(deactivate);
    handler.handleEvent(delete);

    // then
    verifyPublishedResources(1, Map.of(
        "http://localhost:4502/content/we-retail/us/en", "dam:Asset",
        "/content/wknd/us/en", "dam:Asset"
    ));

    // and
    verifyUnpublishedResources(1, Map.of(
        "http://localhost:4502/content/we-retail/us/pl", "dam:Asset",
        "/content/wknd/us/pl", "dam:Asset"
    ));
  }

  @Test
  void shouldNotHandleUnexpectedEvents() throws Exception {
    // given
    Event event = new Event(
        "some/unexpected/event",
        Map.of("type", "UNEXPECTED_ACTION")
    );

    // when
    handler.handleEvent(event);

    // then
    verifyNoPublishedResources();
    verifyNoUnpublishedResources();
  }

  @Test
  void shouldAdjustToModifiedConfiguration() throws ReflectiveOperationException {
    // given
    assertPropertiesToLoadFromJcr(handler, "jcr:primaryType");

    // when
    MockOsgi.modified(handler, context.bundleContext(), Map.of("properties.to.load.from.jcr", new String[]{"foobar"}));

    // then
    assertPropertiesToLoadFromJcr(handler, "foobar");
  }
}