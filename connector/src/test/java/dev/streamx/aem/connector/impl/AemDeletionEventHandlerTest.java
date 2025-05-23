package dev.streamx.aem.connector.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import dev.streamx.aem.connector.test.util.ResourceResolverFactoryMocks;
import dev.streamx.sling.connector.ResourceInfo;
import dev.streamx.sling.connector.StreamxPublicationService;
import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.osgi.service.event.Event;

@ExtendWith({MockitoExtension.class, AemContextExtension.class})
class AemDeletionEventHandlerTest {

  private final AemContext context = new AemContext();
  private final StreamxPublicationService streamxPublicationServiceMock = mock(StreamxPublicationService.class);
  private final ArgumentCaptor<List<ResourceInfo>> unpublishedResourcesCaptor = ArgumentCaptor.forClass(List.class);
  private AemDeletionEventHandler handler;

  @BeforeEach
  void setup() throws Exception {
    doReturn(true).when(streamxPublicationServiceMock).isEnabled();

    handler = new AemDeletionEventHandler(
        streamxPublicationServiceMock,
        ResourceResolverFactoryMocks.withFixedResourcePrimaryNodeType("cq:Page", context)
    );
  }

  @Test
  void test() throws Exception {
    // given
    Event preDeleteResource1Event = new Event(
        "com/adobe/cq/resource/delete",
        Map.of(
            "type", "preDelete",
            "path", "http://localhost:4502/content/we-retail/us/en",
            "userId", "admin"
        )
    );

    Event preDeleteResource2Event = new Event(
        "com/adobe/cq/resource/delete",
        Map.of(
            "type", "preDelete",
            "path", "http://localhost:4502/content/we-purchase/us/en",
            "userId", "admin"
        )
    );

    Event postDeleteResource3Event = new Event(
        "com/adobe/cq/resource/delete",
        Map.of(
            "type", "postDelete",
            "path", "http://localhost:4502/content/we-sell/us/en",
            "userId", "admin"
        )
    );

    // when
    handler.handleEvent(preDeleteResource1Event);
    handler.handleEvent(preDeleteResource2Event);
    handler.handleEvent(postDeleteResource3Event);

    // then
    verify(streamxPublicationServiceMock, times(2)).unpublish(unpublishedResourcesCaptor.capture());
    List<ResourceInfo> allUnpublishedResources = unpublishedResourcesCaptor.getAllValues().stream()
        .flatMap(Collection::stream)
        .collect(Collectors.toList());

    assertThat(allUnpublishedResources).hasSize(2);
    assertResource(allUnpublishedResources.get(0), "http://localhost:4502/content/we-retail/us/en", "cq:Page");
    assertResource(allUnpublishedResources.get(1), "http://localhost:4502/content/we-purchase/us/en", "cq:Page");

    // and
    verify(streamxPublicationServiceMock, never()).publish(anyList());
  }

  private static void assertResource(ResourceInfo resource, String expectedPath, String expectedPrimaryNodeType) {
    assertThat(resource.getPath()).isEqualTo(expectedPath);
    assertThat(resource.getPrimaryNodeType()).isEqualTo(expectedPrimaryNodeType);
  }
}