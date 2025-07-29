package dev.streamx.aem.connector.impl;

import com.day.cq.replication.ReplicationAction;
import com.day.cq.replication.ReplicationActionType;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;
import java.util.Map;
import org.apache.sling.testing.mock.osgi.MockOsgi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.osgi.service.event.Event;

@ExtendWith(AemContextExtension.class)
class AemReplicationEventHandlerTest extends BaseAemEventHandlerTest {

  private AemReplicationEventHandler handler;

  @BeforeEach
  void setup() {
    handler = context.registerInjectActivateService(
        AemReplicationEventHandler.class,
        Map.of("resource.properties.to.load", new String[]{"jcr:primaryType"})
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
                "/content/we-retail/us/en",
                "/content/wknd/us/en"
            },
            "userId", "admin"
        )
    );
    registerResource("/content/we-retail/us/en", "dam:Asset");
    registerResource("/content/wknd/us/en", "dam:Asset");

    Event deactivate = new Event(
        ReplicationAction.EVENT_TOPIC,
        Map.of(
            "type", ReplicationActionType.DEACTIVATE.getName(),
            "paths", new String[]{
                "/content/we-retail/us/pl",
                "/content/wknd/us/pl"
            },
            "userId", "admin"
        )
    );
    registerResource("/content/we-retail/us/pl", "dam:Asset");
    registerResource("/content/wknd/us/pl", "dam:Asset");

    Event delete = new Event(
        ReplicationAction.EVENT_TOPIC,
        Map.of(
            "type", ReplicationActionType.DELETE.getName(),
            "paths", new String[]{
                "/content/we-retail/us/fr",
                "/content/wknd/us/fr"
            },
            "userId", "admin"
        )
    );
    registerResource("/content/we-retail/us/fr", "dam:Asset");
    registerResource("/content/wknd/us/fr", "dam:Asset");

    // when
    handler.handleEvent(activate);
    handler.handleEvent(deactivate);
    handler.handleEvent(delete);

    // then
    verifyPublishedResources(1, Map.of(
        "/content/we-retail/us/en", "dam:Asset",
        "/content/wknd/us/en", "dam:Asset"
    ));

    // and
    verifyUnpublishedResources(1, Map.of(
        "/content/we-retail/us/pl", "dam:Asset",
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
  void shouldAdjustToModifiedConfiguration() {
    // given
    assertResourcePropertiesToLoad(handler, "jcr:primaryType");

    // when
    MockOsgi.modified(handler, context.bundleContext(), Map.of("resource.properties.to.load", new String[]{"foobar"}));

    // then
    assertResourcePropertiesToLoad(handler, "foobar");
  }
}