package dev.streamx.aem.connector.impl;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.day.cq.replication.ReplicationAction;
import dev.streamx.sling.connector.StreamxPublicationException;
import dev.streamx.sling.connector.StreamxPublicationService;
import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.math.NumberUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.osgi.service.event.Event;

@ExtendWith({MockitoExtension.class, AemContextExtension.class})
class AemReplicationEventHandlerTest {

  private final AemContext context = new AemContext();
  private AemReplicationEventHandler handler;
  private List<String> publishedPaths;
  private List<String> unpublishedPaths;

  @SuppressWarnings("ReturnOfNull")
  @BeforeEach
  void setup() throws StreamxPublicationException {
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
    handler.handleEvent(activate);
    handler.handleEvent(deactivate);
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
