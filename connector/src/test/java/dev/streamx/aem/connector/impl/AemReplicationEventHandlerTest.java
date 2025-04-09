package dev.streamx.aem.connector.impl;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.day.cq.commons.Externalizer;
import com.day.cq.replication.ReplicationAction;
import com.day.cq.replication.ReplicationActionType;
import dev.streamx.sling.connector.StreamxPublicationException;
import dev.streamx.sling.connector.StreamxPublicationService;
import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.sling.discovery.DiscoveryService;
import org.apache.sling.discovery.InstanceDescription;
import org.apache.sling.discovery.TopologyView;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;

@ExtendWith({MockitoExtension.class, AemContextExtension.class})
class AemReplicationEventHandlerTest {

  private final AemContext context = new AemContext();
  private List<String> publishedPaths;
  private List<String> unpublishedPaths;

  @SuppressWarnings("ReturnOfNull")
  @BeforeEach
  void setup() throws StreamxPublicationException {
    publishedPaths = List.of();
    unpublishedPaths = List.of();
    context.runMode(Externalizer.PUBLISH);
    DiscoveryService discoveryService = new DiscoveryService() {
      @Override
      public TopologyView getTopology() {
        TopologyView topologyView = mock(TopologyView.class);
        InstanceDescription instanceDescription = mock(InstanceDescription.class);
        when(topologyView.getLocalInstance()).thenReturn(instanceDescription);
        when(instanceDescription.isLeader()).thenReturn(true);
        return topologyView;
      }
    };
    context.registerService(DiscoveryService.class, discoveryService);
    StreamxPublicationService streamxPublicationService = mock(StreamxPublicationService.class);
    doAnswer(
        invocation -> {
          @SuppressWarnings("unchecked")
          List<String> paths = (List<String>) invocation.getArgument(
              NumberUtils.INTEGER_ZERO, List.class
          );
          publishedPaths = paths;
          return null;
        }
    ).when(streamxPublicationService).publish(anyList());
    doAnswer(
        invocation -> {
          @SuppressWarnings("unchecked")
          List<String> paths = (List<String>) invocation.getArgument(
              NumberUtils.INTEGER_ZERO, List.class
          );
          unpublishedPaths = paths;
          return null;
        }
    ).when(streamxPublicationService).unpublish(anyList());
    when(streamxPublicationService.isEnabled()).thenReturn(true);
    context.registerService(StreamxPublicationService.class, streamxPublicationService);
    context.registerInjectActivateService(
        AemReplicationEventHandler.class, Map.of("push-from", Externalizer.PUBLISH)
    );
  }

  @Test
  void consumeEvents() {
    Event activate = new Event(
        ReplicationAction.EVENT_TOPIC,
        Map.of(
            "type", "Activate",
            "paths", new String[]{
                "http://localhost:4502/content/we-retail/us/en",
                "/content/wknd/us/en"
            },
            "userId", "admin"
        )
    );
    Map<String, Object> props = new HashMap<>();
    props.put(
        "paths", new String[]{
            "http://localhost:4502/content/we-retail/us/pl",
            "/content/wknd/us/pl"
        }
    );
    props.put("type", ReplicationActionType.DEACTIVATE);
    Collection<Map<String, Object>> modifications = new ArrayList<>();
    modifications.add(props);
    Event deactivate = new Event(
        "com/adobe/granite/replication",
        Map.of(
            "modifications", modifications,
            "userId", "admin"
        )
    );
    EventAdmin eventAdmin = Objects.requireNonNull(context.getService(EventAdmin.class));
    eventAdmin.sendEvent(activate);
    eventAdmin.sendEvent(deactivate);
    assertAll(
        () -> assertEquals(
            List.of("http://localhost:4502/content/we-retail/us/en", "/content/wknd/us/en"),
            publishedPaths
        ),
        () -> assertEquals(
            List.of("http://localhost:4502/content/we-retail/us/pl", "/content/wknd/us/pl"),
            unpublishedPaths
        )
    );
  }
}
