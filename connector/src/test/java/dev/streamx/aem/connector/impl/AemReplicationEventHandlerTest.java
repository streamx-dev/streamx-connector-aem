package dev.streamx.aem.connector.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.day.cq.replication.ReplicationAction;
import com.day.cq.replication.ReplicationActionType;
import dev.streamx.aem.connector.test.util.ResourceResolverFactoryMocks;
import dev.streamx.sling.connector.ResourceToIngest;
import dev.streamx.sling.connector.StreamxPublicationException;
import dev.streamx.sling.connector.StreamxPublicationService;
import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.osgi.service.event.Event;

@ExtendWith({MockitoExtension.class, AemContextExtension.class})
class AemReplicationEventHandlerTest {

  private final AemContext context = new AemContext();
  private AemReplicationEventHandler handler;
  private List<ResourceToIngest> publishedResources;
  private List<ResourceToIngest> unpublishedResources;

  @SuppressWarnings("ReturnOfNull")
  @BeforeEach
  void setup() throws Exception {
    handler = new AemReplicationEventHandler(
        createStreamxPublicationServiceMock(),
        ResourceResolverFactoryMocks.withFixedResourcePrimaryNodeType("dam:Asset", context)
    );
  }

  private StreamxPublicationService createStreamxPublicationServiceMock() throws StreamxPublicationException {
    StreamxPublicationService streamxPublicationService = mock(StreamxPublicationService.class);
    doAnswer(
        invocation -> {
          @SuppressWarnings("unchecked")
          List<ResourceToIngest> resources = (List<ResourceToIngest>) invocation.getArgument(0, List.class);
          publishedResources = resources;
          return null;
        }
    ).when(streamxPublicationService).publish(anyList());
    doAnswer(
        invocation -> {
          @SuppressWarnings("unchecked")
          List<ResourceToIngest> resources = (List<ResourceToIngest>) invocation.getArgument(0, List.class);
          unpublishedResources = resources;
          return null;
        }
    ).when(streamxPublicationService).unpublish(anyList());
    when(streamxPublicationService.isEnabled()).thenReturn(true);
    return streamxPublicationService;
  }

  @Test
  void test() {
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
    handler.handleEvent(activate);
    handler.handleEvent(deactivate);
    handler.handleEvent(delete);

    assertThat(publishedResources).hasSize(2);
    assertResource(publishedResources.get(0), "http://localhost:4502/content/we-retail/us/en", "dam:Asset");
    assertResource(publishedResources.get(1), "/content/wknd/us/en", "dam:Asset");

    assertThat(unpublishedResources).hasSize(2);
    assertResource(unpublishedResources.get(0), "http://localhost:4502/content/we-retail/us/pl", "dam:Asset");
    assertResource(unpublishedResources.get(1), "/content/wknd/us/pl", "dam:Asset");
  }

  private static void assertResource(ResourceToIngest resource, String expectedPath, String expectedPrimaryNodeType) {
    assertThat(resource.getPath()).isEqualTo(expectedPath);
    assertThat(resource.getPrimaryNodeType()).isEqualTo(expectedPrimaryNodeType);
  }
}
