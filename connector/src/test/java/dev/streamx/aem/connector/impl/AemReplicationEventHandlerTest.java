package dev.streamx.aem.connector.impl;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.day.cq.replication.ReplicationAction;
import dev.streamx.sling.connector.IngestionTrigger;
import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.sling.event.jobs.Job;
import org.apache.sling.event.jobs.JobManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.osgi.service.event.Event;

@ExtendWith({MockitoExtension.class, AemContextExtension.class})
class AemReplicationEventHandlerTest {

  private final AemContext context = new AemContext();

  @Mock
  private JobManager jobManager;

  private AtomicReference<String> jobTopic;
  private Map<String, Object> jobProps;
  private AtomicInteger numberOfRuns;
  private AemReplicationEventHandler handler;


  @BeforeEach
  void setup() {
    jobTopic = new AtomicReference<>(StringUtils.EMPTY);
    jobProps = Map.of();
    numberOfRuns = new AtomicInteger();
    when(jobManager.addJob(anyString(), anyMap())).then(
        invocation -> {
          jobTopic.set(invocation.getArgument(NumberUtils.INTEGER_ZERO));
          jobProps = invocation.getArgument(NumberUtils.INTEGER_ONE);
          numberOfRuns.incrementAndGet();
          return mock(Job.class);
        }
    );
    context.registerService(JobManager.class, jobManager);
    handler = context.registerInjectActivateService(AemReplicationEventHandler.class);
  }

  @Test
  void submitJob() {
    Event event = new Event(
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
    handler.handleEvent(event);
    assertAll(
        () -> assertEquals(IngestionTrigger.JOB_TOPIC, jobTopic.get()),
        () -> assertEquals(NumberUtils.INTEGER_ONE, numberOfRuns.get()),
        () -> assertEquals(NumberUtils.INTEGER_TWO, jobProps.size()),
        () -> assertTrue(jobProps.containsKey("streamx.ingestionAction")),
        () -> assertTrue(jobProps.containsKey("streamx.urisToIngest"))
    );
  }
}
