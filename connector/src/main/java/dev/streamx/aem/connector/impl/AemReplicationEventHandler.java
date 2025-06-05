package dev.streamx.aem.connector.impl;

import com.day.cq.replication.ReplicationAction;
import com.day.cq.replication.ReplicationActionType;
import dev.streamx.sling.connector.PublicationAction;
import dev.streamx.sling.connector.ResourceInfo;
import dev.streamx.sling.connector.StreamxPublicationException;
import dev.streamx.sling.connector.StreamxPublicationService;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
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
    ReplicationAction replicationAction = ReplicationAction.fromEvent(event);
    if (replicationAction == null) {
      LOG.warn("Cannot get action from {}", event);
      return;
    }

    ReplicationActionType replicationActionType = replicationAction.getType();
    if (!actionsMap.containsKey(replicationActionType)) {
      LOG.warn("Unhandled action: {}", replicationAction);
      return;
    }

    PublicationAction ingestionAction = actionsMap.get(replicationActionType);
    List<ResourceInfo> resourcesToIngest = getResourcesToIngest(replicationAction.getPaths());
    handleIngestion(ingestionAction, resourcesToIngest);
  }

  private List<ResourceInfo> getResourcesToIngest(String[] paths) {
    try (ResourceResolver resourceResolver = createResourceResolver()) {
      return Arrays.stream(paths)
          .map(path -> new ResourceInfo(path, PrimaryNodeTypeExtractor.extract(path, resourceResolver)))
          .collect(Collectors.toList());
    }
  }

  private void handleIngestion(PublicationAction ingestionAction, List<ResourceInfo> resourcesToIngest) {
    List<String> paths = resourcesToIngest.stream().map(ResourceInfo::getPath).collect(Collectors.toList());
    LOG.trace("Handling ingestion {} for {}", ingestionAction, paths);

    try {
      if (ingestionAction == PublicationAction.PUBLISH) {
        streamxPublicationService.publish(resourcesToIngest);
      } else if (ingestionAction == PublicationAction.UNPUBLISH) {
        streamxPublicationService.unpublish(resourcesToIngest);
      }
    } catch (StreamxPublicationException exception) {
      LOG.error("Failed to handle ingestion {} for {}", ingestionAction, paths, exception);
    }
  }
}
