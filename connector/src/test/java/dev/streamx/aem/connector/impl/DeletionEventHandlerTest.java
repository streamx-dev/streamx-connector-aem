package dev.streamx.aem.connector.impl;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

import com.adobe.aem.formsndocuments.util.FMConstants;
import com.day.cq.commons.jcr.JcrConstants;
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
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.sling.jcr.resource.api.JcrResourceConstants;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;

@ExtendWith({MockitoExtension.class, AemContextExtension.class})
class DeletionEventHandlerTest {

  private final AemContext context = new AemContext(ResourceResolverType.JCR_OAK);
  private DeletionEventHandler handler;
  private List<IngestedData> passedIngestedData;

  @SuppressWarnings("ReturnOfNull")
  @BeforeEach
  void setup() {
    passedIngestedData = new ArrayList<>();
    StreamxPublicationService streamxPublicationService = mock(StreamxPublicationService.class);
    doAnswer(
        invocation -> {
          IngestedData ingestedData = invocation.getArgument(
              NumberUtils.INTEGER_ZERO, IngestedData.class
          );
          passedIngestedData.add(ingestedData);
          return null;
        }
    ).when(streamxPublicationService).ingest(any(IngestedData.class));
    context.registerService(StreamxPublicationService.class, streamxPublicationService);
    handler = context.registerInjectActivateService(
        DeletionEventHandler.class, Map.of(
            "property-name.regex", String.format("%s|streamx.*", JcrConstants.JCR_PRIMARYTYPE)
        )
    );
    context.build().resource(
        "/content/wknd/us/en", Map.of(
            JcrConstants.JCR_PRIMARYTYPE, FMConstants.CQ_PAGE_NODETYPE,
            JcrConstants.JCR_CREATED_BY, "userus"
        )
    ).commit();
    context.build().resource(
        "/content/wknd/us/de", Map.of(
            JcrConstants.JCR_PRIMARYTYPE, JcrResourceConstants.NT_SLING_FOLDER,
            "streamxProperty", "streamxValue"
        )
    ).commit();
  }

  @Test
  void generalTest() {
    Event relevantOne = new Event(
        "com/adobe/cq/resource/delete",
        Map.of(
            "type", "preDelete",
            "path", "/content/wknd/us/en",
            "userId", "admin"
        )
    );
    Event relevantTwo = new Event(
        "com/adobe/cq/resource/delete",
        Map.of(
            "type", "preDelete",
            "path", "/content/wknd/us/de",
            "userId", "admin"
        )
    );
    Event irrelevantOne = new Event(
        "com/adobe/cq/resource/delete",
        Map.of(
            "type", "unknown",
            "path", "/content/wknd/us/en",
            "userId", "admin"
        )
    );
    Event irrelevantTwo = new Event(
        ReplicationAction.EVENT_TOPIC,
        Map.of(
            "type", "preDelete",
            "path", "/content/wknd/us/de",
            "userId", "admin"
        )
    );
    EventAdmin eventAdmin = Objects.requireNonNull(context.getService(EventAdmin.class));
    eventAdmin.sendEvent(relevantOne);
    eventAdmin.sendEvent(relevantTwo);
    eventAdmin.sendEvent(irrelevantOne);
    eventAdmin.sendEvent(irrelevantTwo);
    IngestedData firstID = passedIngestedData.get(NumberUtils.INTEGER_ZERO);
    IngestedData secondID = passedIngestedData.get(NumberUtils.INTEGER_ONE);
    assertAll(
        () -> assertEquals(2, passedIngestedData.size()),
        () -> assertEquals("/content/wknd/us/en", firstID.uriToIngest().toString()),
        () -> assertEquals("/content/wknd/us/de", secondID.uriToIngest().toString()),
        () -> assertEquals(PublicationAction.UNPUBLISH, firstID.ingestionAction()),
        () -> assertEquals(PublicationAction.UNPUBLISH, secondID.ingestionAction()),
        () -> assertAll(
            () -> assertEquals(1, firstID.properties().size()),
            () -> assertEquals(FMConstants.CQ_PAGE_NODETYPE, firstID.properties().get("streamx.jcr:primaryType"))
        ),
        () -> assertAll(
            () -> assertEquals(2, secondID.properties().size()),
            () -> assertEquals(JcrResourceConstants.NT_SLING_FOLDER, secondID.properties().get("streamx.jcr:primaryType")),
            () -> assertEquals("streamxValue", secondID.properties().get("streamx.streamxProperty"))
        )
    );
  }
}
