package dev.streamx.aem.connector.impl;

import com.day.cq.replication.ReplicationAction;
import com.day.cq.replication.ReplicationActionType;
import dev.streamx.sling.connector.PublicationAction;
import dev.streamx.sling.connector.ResourceInfo;
import dev.streamx.sling.connector.StreamxPublicationService;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(
    service = EventHandler.class,
    immediate = true,
    property = EventConstants.EVENT_TOPIC + "=" + ReplicationAction.EVENT_TOPIC
)
@Designate(ocd = AemReplicationEventHandlerConfig.class)
public class AemReplicationEventHandler extends BaseAemEventHandler {

  private static final Logger LOG = LoggerFactory.getLogger(AemReplicationEventHandler.class);

  private static final Map<ReplicationActionType, PublicationAction> actionsMap = Map.of(
      ReplicationActionType.ACTIVATE, PublicationAction.PUBLISH,
      ReplicationActionType.DEACTIVATE, PublicationAction.UNPUBLISH
  );

  @Activate
  public AemReplicationEventHandler(
      @Reference StreamxPublicationService streamxPublicationService,
      @Reference ResourceResolverFactory resourceResolverFactory,
      AemReplicationEventHandlerConfig config
  ) {
    super(streamxPublicationService, resourceResolverFactory, config.resource_properties_to_load());
  }

  @Modified
  void configure(AemReplicationEventHandlerConfig config) {
    super.configure(config.resource_properties_to_load());
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
          .map(path -> new ResourceInfo(path, loadResourceProperties(path, resourceResolver)))
          .collect(Collectors.toList());
    }
  }

  private void handleIngestion(PublicationAction ingestionAction, List<ResourceInfo> resourcesToIngest) {
    if (ingestionAction == PublicationAction.PUBLISH) {
      publish(resourcesToIngest);
    } else if (ingestionAction == PublicationAction.UNPUBLISH) {
      unpublish(resourcesToIngest);
    }
  }
}
