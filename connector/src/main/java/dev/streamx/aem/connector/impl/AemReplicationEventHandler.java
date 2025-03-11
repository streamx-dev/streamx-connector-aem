package dev.streamx.aem.connector.impl;

import com.day.cq.replication.ReplicationAction;
import com.day.cq.replication.ReplicationActionType;
import dev.streamx.sling.connector.PublicationAction;
import dev.streamx.sling.connector.StreamxPublicationException;
import dev.streamx.sling.connector.StreamxPublicationService;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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

  private final StreamxPublicationService streamxPublicationService;
  private final Map<ReplicationActionType, PublicationAction> actionsMap;

  @Activate
  public AemReplicationEventHandler(
      @Reference(cardinality = ReferenceCardinality.MANDATORY)
      StreamxPublicationService streamxPublicationService
  ) {
    this.streamxPublicationService = streamxPublicationService;
    actionsMap = Map.of(
        ReplicationActionType.ACTIVATE, PublicationAction.PUBLISH,
        ReplicationActionType.DEACTIVATE, PublicationAction.UNPUBLISH,
        ReplicationActionType.DELETE, PublicationAction.UNPUBLISH
    );
  }

  @Override
  public void handleEvent(Event event) {
    LOG.trace("Received {}", event);
    if (streamxPublicationService.isEnabled()) {
      Optional.ofNullable(ReplicationAction.fromEvent(event))
          .ifPresentOrElse(
              this::handleAction, () -> LOG.warn("Cannot get action from {}", event)
          );
    } else {
      LOG.trace("{} is disabled. Ignoring {}", StreamxPublicationService.class, event);
    }
  }

  private void handleAction(ReplicationAction action) {
    ReplicationActionType replicationActionType = action.getType();
    List<String> paths = List.of(action.getPaths());
    Optional.ofNullable(actionsMap.get(replicationActionType))
        .ifPresentOrElse(
            ingestionAction -> handleIngestion(ingestionAction, paths),
            () -> LOG.warn("Failed to add job for: {}", action)
        );
  }

  private void handleIngestion(PublicationAction ingestionAction, List<String> paths) {
    LOG.trace("Handling ingestion {} for {}", ingestionAction, paths);
    try {
      if (ingestionAction == PublicationAction.PUBLISH) {
        streamxPublicationService.publish(paths);
      } else if (ingestionAction == PublicationAction.UNPUBLISH) {
        streamxPublicationService.unpublish(paths);
      } else {
        LOG.warn("Unknown ingestion action: {}. Ignored paths: {}", ingestionAction, paths);
      }
    } catch (StreamxPublicationException exception) {
      String message = String.format(
          "Failed to handle ingestion %s for %s", ingestionAction, paths
      );
      LOG.error(message, exception);
    }
  }
}
