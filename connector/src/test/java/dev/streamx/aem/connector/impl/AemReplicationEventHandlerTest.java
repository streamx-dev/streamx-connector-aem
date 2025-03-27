package dev.streamx.aem.connector.impl;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.day.cq.replication.ReplicationAction;
import dev.streamx.sling.connector.IngestedData;
import dev.streamx.sling.connector.PublicationAction;
import dev.streamx.sling.connector.StreamxPublicationService;
import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.apache.commons.lang3.math.NumberUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;

@ExtendWith({MockitoExtension.class, AemContextExtension.class})
class AemReplicationEventHandlerTest {

  private final AemContext context = new AemContext();
  private AemReplicationEventHandler handler;
  private List<String> publishedPaths;
  private List<String> unpublishedPaths;

  @SuppressWarnings("ReturnOfNull")
  @BeforeEach
  void setup() {
    publishedPaths = new ArrayList<>();
    unpublishedPaths = new ArrayList<>();
    StreamxPublicationService streamxPublicationService = mock(StreamxPublicationService.class);
    doAnswer(
        invocation -> {
          IngestedData ingestedData = invocation.getArgument(
              NumberUtils.INTEGER_ZERO, IngestedData.class
          );
          Optional.of(ingestedData)
              .filter(data -> data.ingestionAction() == PublicationAction.PUBLISH)
              .ifPresent(data -> publishedPaths.add(data.uriToIngest().toString()));
          Optional.of(ingestedData)
              .filter(data -> data.ingestionAction() == PublicationAction.UNPUBLISH)
              .ifPresent(data -> unpublishedPaths.add(data.uriToIngest().toString()));
          return null;
        }
    ).when(streamxPublicationService).ingest(any(IngestedData.class));
    when(streamxPublicationService.isEnabled()).thenReturn(true);
    context.registerService(StreamxPublicationService.class, streamxPublicationService);
    handler = context.registerInjectActivateService(AemReplicationEventHandler.class);
  }

  @Test
  void test() {
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
    Event deactivate = new Event(
        ReplicationAction.EVENT_TOPIC,
        Map.of(
            "type", "Deactivate",
            "paths", new String[]{
                "http://localhost:4502/content/we-retail/us/pl",
                "/content/wknd/us/pl"
            },
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
