package dev.streamx.aem.connector.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
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
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.osgi.service.event.Event;

@ExtendWith(AemContextExtension.class)
class AemDeletionEventHandlerTest {

  private final AemContext context = new AemContext();
  private final StreamxPublicationService streamxPublicationServiceMock = mock(StreamxPublicationService.class);
  private final ArgumentCaptor<List<ResourceInfo>> unpublishedResourcesCaptor = ArgumentCaptor.forClass(List.class);

  @Test
  void shouldHandleEvents() throws Exception {
    // given
    doReturn(true).when(streamxPublicationServiceMock).isEnabled();

    AemDeletionEventHandler handler = new AemDeletionEventHandler(
        streamxPublicationServiceMock,
        ResourceResolverFactoryMocks.withFixedResourcePrimaryNodeType("cq:Page", context)
    );

    // when
    handler.handleEvent(createDeleteEvent("preDelete", "http://localhost:4502/content/we-retail/us/en"));
    handler.handleEvent(createDeleteEvent("preDelete", "http://localhost:4502/content/we-purchase/us/en"));
    handler.handleEvent(createDeleteEvent("postDelete", "http://localhost:4502/content/we-sell/us/en"));

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

  @Test
  void shouldSkipHandlingEventIfPublicationServiceIsDisabled() throws Exception {
    // given
    doReturn(false).when(streamxPublicationServiceMock).isEnabled();

    AemDeletionEventHandler handler = spy(new AemDeletionEventHandler(
        streamxPublicationServiceMock,
        mock(ResourceResolverFactory.class)
    ));

    // when
    handler.handleEvent(createDeleteEvent("preDelete", "http://localhost:4502/content/we-retail/us/en"));

    // then
    verify(handler, never()).createResourceResolver();
    verify(streamxPublicationServiceMock, never()).unpublish(anyList());
    verify(streamxPublicationServiceMock, never()).publish(anyList());
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

  private static void assertResource(ResourceInfo resource, String expectedPath, String expectedPrimaryNodeType) {
    assertThat(resource.getPath()).isEqualTo(expectedPath);
    assertThat(resource.getPrimaryNodeType()).isEqualTo(expectedPrimaryNodeType);
  }
}