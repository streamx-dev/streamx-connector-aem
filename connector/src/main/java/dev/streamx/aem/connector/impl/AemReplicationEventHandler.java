package dev.streamx.aem.connector.impl;

import com.day.cq.replication.ReplicationAction;
import com.day.cq.replication.ReplicationActionType;
import dev.streamx.sling.connector.IngestionTrigger;
import dev.streamx.sling.connector.PublicationAction;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.uri.SlingUri;
import org.apache.sling.api.uri.SlingUriBuilder;
import org.apache.sling.event.jobs.JobManager;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(
    service = EventHandler.class,
    immediate = true,
    property = EventConstants.EVENT_TOPIC + "=" + ReplicationAction.EVENT_TOPIC
)
public class AemReplicationEventHandler implements EventHandler {

  private static final Logger LOG = LoggerFactory.getLogger(AemReplicationEventHandler.class);

  private final ResourceResolverFactory resourceResolverFactory;
  private final JobManager jobManager;
  private final Map<ReplicationActionType, PublicationAction> actionsMap;

  @Activate
  public AemReplicationEventHandler(
      @Reference(cardinality = ReferenceCardinality.MANDATORY)
      ResourceResolverFactory resourceResolverFactory,
      @Reference(cardinality = ReferenceCardinality.MANDATORY)
      JobManager jobManager
  ) {
    this.resourceResolverFactory = resourceResolverFactory;
    this.jobManager = jobManager;
    actionsMap = Map.of(
        ReplicationActionType.ACTIVATE, PublicationAction.PUBLISH,
        ReplicationActionType.DEACTIVATE, PublicationAction.UNPUBLISH,
        ReplicationActionType.DELETE, PublicationAction.UNPUBLISH
    );
  }

  @Override
  public void handleEvent(Event event) {
    LOG.trace("Received {}", event);
    Optional.ofNullable(ReplicationAction.fromEvent(event))
        .ifPresentOrElse(
            this::submitIngestionTriggerJob, () -> LOG.warn("Cannot get action from {}", event)
        );
  }

  private void submitIngestionTriggerJob(ReplicationAction action) {
    ReplicationActionType replicationActionType = action.getType();
    List<SlingUri> slingUris = Stream.of(action.getPaths())
        .map(this::toSlingUri)
        .flatMap(Optional::stream)
        .collect(Collectors.toUnmodifiableList());
    Optional.ofNullable(actionsMap.get(replicationActionType))
        .map(ingestionAction -> new IngestionTrigger(ingestionAction, slingUris))
        .map(IngestionTrigger::asJobProps)
        .map(jobProps -> jobManager.addJob(IngestionTrigger.JOB_TOPIC, jobProps))
        .ifPresentOrElse(
            job -> LOG.debug("Added job: {}", job),
            () -> LOG.warn("Failed to add job for: {}", action)
        );
  }

  @SuppressWarnings({"squid:S1874", "deprecation"})
  private Optional<SlingUri> toSlingUri(String rawUri) {
    try (
        ResourceResolver resourceResolver
            = resourceResolverFactory.getAdministrativeResourceResolver(null)
    ) {
      SlingUri slingUri = SlingUriBuilder.parse(rawUri, resourceResolver).build();
      LOG.trace("Parsed URI: {}", slingUri);
      return Optional.of(slingUri);
    } catch (LoginException exception) {
      String message = String.format("Unable to parse URI: '%s'", rawUri);
      LOG.error(message, exception);
      return Optional.empty();
    }
  }
}
