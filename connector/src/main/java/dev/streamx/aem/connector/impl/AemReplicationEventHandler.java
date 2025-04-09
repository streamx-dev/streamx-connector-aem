package dev.streamx.aem.connector.impl;

import com.day.cq.replication.ReplicationAction;
import com.day.cq.replication.ReplicationActionType;
import dev.streamx.sling.connector.PublicationAction;
import dev.streamx.sling.connector.StreamxPublicationException;
import dev.streamx.sling.connector.StreamxPublicationService;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import org.apache.sling.discovery.DiscoveryService;
import org.apache.sling.discovery.InstanceDescription;
import org.apache.sling.settings.SlingSettingsService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(
    service = EventHandler.class,
    immediate = true,
    property = {
        EventConstants.EVENT_TOPIC + "=" + ReplicationAction.EVENT_TOPIC,
        EventConstants.EVENT_TOPIC + "=" + "com/adobe/granite/replication"
    },
    configurationPolicy = ConfigurationPolicy.REQUIRE
)
@Designate(ocd = AemReplicationEventHandlerConfig.class)
public class AemReplicationEventHandler implements EventHandler {

  private static final Logger LOG = LoggerFactory.getLogger(AemReplicationEventHandler.class);

  private final DiscoveryService discoveryService;
  private final StreamxPublicationService streamxPublicationService;
  private final SlingSettingsService slingSettingsService;
  private final Map<ReplicationActionType, PublicationAction> actionsMap;
  private final AtomicReference<AemReplicationEventHandlerConfig> config;

  @Activate
  public AemReplicationEventHandler(
      @Reference(cardinality = ReferenceCardinality.MANDATORY)
      DiscoveryService discoveryService,
      @Reference(cardinality = ReferenceCardinality.MANDATORY)
      StreamxPublicationService streamxPublicationService,
      @Reference(cardinality = ReferenceCardinality.MANDATORY)
      SlingSettingsService slingSettingsService,
      AemReplicationEventHandlerConfig config
  ) {

    this.discoveryService = discoveryService;
    this.streamxPublicationService = streamxPublicationService;
    this.slingSettingsService = slingSettingsService;
    this.actionsMap = Map.of(
        ReplicationActionType.ACTIVATE, PublicationAction.PUBLISH,
        ReplicationActionType.DEACTIVATE, PublicationAction.UNPUBLISH,
        ReplicationActionType.DELETE, PublicationAction.UNPUBLISH
    );
    this.config = new AtomicReference<>(config);
  }

  @Modified
  void configure(AemReplicationEventHandlerConfig config) {
    this.config.set(config);
  }

  @Override
  public void handleEvent(Event event) {
    LOG.trace("Received {}", event);
    Optional.of(event)
        .filter(
            replicationAction -> {
              InstanceDescription localInstance = discoveryService.getTopology().getLocalInstance();
              boolean isLeader = localInstance.isLeader();
              LOG.trace(
                  "Is this {} a leader? Answer: {}. Event: {}", localInstance, isLeader, event
              );
              return isLeader;
            }
        ).filter(
            replicationAction -> {
              boolean isEnabled = streamxPublicationService.isEnabled();
              LOG.trace(
                  "Is {} enabled? Answer: {}. Event: {}", StreamxPublicationService.class,
                  isEnabled, event
              );
              return isEnabled;
            }
        ).filter(
            replicationAction -> {
              Set<String> runModes = slingSettingsService.getRunModes().stream()
                  .map(String::toLowerCase)
                  .collect(Collectors.toUnmodifiableSet());
              String expectedRunMode = config.get().push$_$from().toLowerCase();
              boolean isExpectedRunMode = runModes.contains(expectedRunMode);
              LOG.trace(
                  "Is this an expected run mode? Answer: {}. Event: {}", isExpectedRunMode, event
              );
              return isExpectedRunMode;
            }
        )
        .map(ReplicationEvents::new)
        .map(ReplicationEvents::get)
        .orElse(List.of())
        .forEach(this::handleAction);
  }

  private void handleAction(ReplicationEvent replicationEvent) {
    ReplicationActionType replicationActionType = replicationEvent.replicationActionType();
    List<String> paths = replicationEvent.paths();
    LOG.trace("Handling {} for {}", replicationActionType, paths);
    Optional.ofNullable(actionsMap.get(replicationActionType))
        .ifPresentOrElse(
            ingestionAction -> handleIngestion(ingestionAction, paths),
            () -> LOG.warn("Failed to add job for: {}", replicationEvent)
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
