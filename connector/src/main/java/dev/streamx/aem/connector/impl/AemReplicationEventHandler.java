package dev.streamx.aem.connector.impl;

import com.day.cq.replication.ReplicationAction;
import com.day.cq.replication.ReplicationActionType;
import dev.streamx.sling.connector.PublicationAction;
import dev.streamx.sling.connector.ResourceInfo;
import dev.streamx.sling.connector.StreamxPublicationException;
import dev.streamx.sling.connector.StreamxPublicationService;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
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
public class AemReplicationEventHandler extends BaseAemEventHandler {

  private static final Logger LOG = LoggerFactory.getLogger(AemReplicationEventHandler.class);

  private static final Map<ReplicationActionType, PublicationAction> actionsMap = Map.of(
      ReplicationActionType.ACTIVATE, PublicationAction.PUBLISH,
      ReplicationActionType.DEACTIVATE, PublicationAction.UNPUBLISH
  );

  @Activate
  public AemReplicationEventHandler(
      @Reference(cardinality = ReferenceCardinality.MANDATORY)
      StreamxPublicationService streamxPublicationService,
      @Reference(cardinality = ReferenceCardinality.MANDATORY)
      ResourceResolverFactory resourceResolverFactory
  ) {
    super(streamxPublicationService, resourceResolverFactory);
  }

  @Override
  protected void doHandleEvent(Event event) {
    Optional.ofNullable(ReplicationAction.fromEvent(event))
        .ifPresentOrElse(
            this::handleAction, () -> LOG.warn("Cannot get action from {}", event)
        );
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
    try (ResourceResolver resourceResolver = createResourceResolver()) {
      List<ResourceInfo> resourcesToIngest = paths.stream()
          .map(path -> new ResourceInfo(
              path,
              PrimaryNodeTypeExtractor.extract(path, resourceResolver)
          ))
          .collect(Collectors.toList());
      if (ingestionAction == PublicationAction.PUBLISH) {
        streamxPublicationService.publish(resourcesToIngest);
      } else if (ingestionAction == PublicationAction.UNPUBLISH) {
        streamxPublicationService.unpublish(resourcesToIngest);
      } else {
        LOG.warn("Unknown ingestion action: {}. Ignored paths: {}", ingestionAction, paths);
      }
    } catch (StreamxPublicationException exception) {
      LOG.error("Failed to handle ingestion {} for {}", ingestionAction, paths, exception);
    }
  }
}
