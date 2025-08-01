package dev.streamx.aem.connector.impl;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import dev.streamx.sling.connector.ResourceInfo;
import dev.streamx.sling.connector.StreamxPublicationService;
import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;
import java.io.ByteArrayInputStream;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

@ExtendWith(AemContextExtension.class)
class BaseAemEventHandlerTest {

  @SuppressWarnings("unchecked")
  private final ArgumentCaptor<List<ResourceInfo>> publishedResourcesCaptor = ArgumentCaptor.forClass(List.class);

  @SuppressWarnings("unchecked")
  private final ArgumentCaptor<List<ResourceInfo>> unpublishedResourcesCaptor = ArgumentCaptor.forClass(List.class);

  private final StreamxPublicationService streamxPublicationService = mock(StreamxPublicationService.class);
  protected final AemContext context = new AemContext();

  @BeforeEach
  void setupStreamxPublicationService() {
    doReturn(true).when(streamxPublicationService).isEnabled();

    context.registerService(
        StreamxPublicationService.class,
        streamxPublicationService
    );
  }

  protected void registerResource(String resourcePath, String primaryNodeType) {
    String resourceJson =
        "{" +
        "  \"jcr:primaryType\": \"" + primaryNodeType + "\"" +
        "}";
    registerCustomResource(resourcePath, resourceJson);
  }

  protected void registerCustomResource(String resourcePath, String resourceJson) {
    var inputStream = new ByteArrayInputStream(resourceJson.getBytes(UTF_8));
    context.load().json(inputStream, resourcePath);
  }

  protected void disableStreamxPublicationService() {
    doReturn(false).when(streamxPublicationService).isEnabled();
  }

  protected List<ResourceInfo> verifyPublishedResources(int expectedNumberOfPublishes, Map<String, String> expectedResources) {
    verify(streamxPublicationService, times(expectedNumberOfPublishes)).publish(publishedResourcesCaptor.capture());
    return verifyIngestedResources(publishedResourcesCaptor, expectedResources);
  }

  protected void verifyNoPublishedResources() {
    verify(streamxPublicationService, never()).publish(anyList());
  }

  protected List<ResourceInfo> verifyUnpublishedResources(int expectedNumberOfUnpublishes, Map<String, String> expectedResources) {
    verify(streamxPublicationService, times(expectedNumberOfUnpublishes)).unpublish(unpublishedResourcesCaptor.capture());
    return verifyIngestedResources(unpublishedResourcesCaptor, expectedResources);
  }

  protected void verifyNoUnpublishedResources() {
    verify(streamxPublicationService, never()).unpublish(anyList());
  }

  private List<ResourceInfo> verifyIngestedResources(ArgumentCaptor<List<ResourceInfo>> captor, Map<String, String> expectedResources) {
    List<ResourceInfo> ingestedResources = captor.getAllValues().stream()
        .flatMap(Collection::stream)
        .sorted(Comparator.comparing(ResourceInfo::getPath))
        .collect(Collectors.toList());

    assertThat(ingestedResources).hasSameSizeAs(expectedResources.entrySet());

    int i = 0;
    for (var expectedResource : new TreeMap<>(expectedResources).entrySet()) {
      ResourceInfo resource = ingestedResources.get(i++);
      assertThat(resource.getPath()).isEqualTo(expectedResource.getKey());
      assertThat(resource.getProperties()).containsEntry("jcr:primaryType", expectedResource.getValue());
    }

    return ingestedResources;
  }

  @SuppressWarnings("unchecked")
  protected void assertResourcePropertiesToLoad(BaseAemEventHandler handler, String... expectedProperties) {
    try {
      Field field = BaseAemEventHandler.class.getDeclaredField("resourcePropertiesToLoad");
      field.setAccessible(true);
      Set<String> actualProperties = ((AtomicReference<Set<String>>) field.get(handler)).get();

      assertThat(actualProperties).containsExactly(expectedProperties);
    } catch (ReflectiveOperationException ex) {
      fail(ex);
    }
  }
}