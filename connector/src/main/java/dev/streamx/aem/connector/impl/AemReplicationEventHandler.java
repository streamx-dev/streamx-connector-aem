package dev.streamx.aem.connector.impl;

import com.day.cq.replication.ReplicationAction;
import com.day.cq.replication.ReplicationActionType;
import dev.streamx.sling.connector.IngestedData;
import dev.streamx.sling.connector.PublicationAction;
import dev.streamx.sling.connector.StreamxPublicationService;
import dev.streamx.sling.connector.util.DefaultSlingUriBuilder;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.uri.SlingUri;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component(
    service = EventHandler.class,
    immediate = true,
    property = EventConstants.EVENT_TOPIC + "=" + ReplicationAction.EVENT_TOPIC
)
public class AemReplicationEventHandler implements EventHandler {

  private static final Logger LOG = LoggerFactory.getLogger(AemReplicationEventHandler.class);

  private final StreamxPublicationService streamxPublicationService;
  private final ResourceResolverFactory resourceResolverFactory;
  private final Map<ReplicationActionType, PublicationAction> actionsMap;

  @Activate
  public AemReplicationEventHandler(
      @Reference(cardinality = ReferenceCardinality.MANDATORY)
      StreamxPublicationService streamxPublicationService,
      @Reference(cardinality = ReferenceCardinality.MANDATORY)
      ResourceResolverFactory resourceResolverFactory
  ) {
    this.streamxPublicationService = streamxPublicationService;
    this.resourceResolverFactory = resourceResolverFactory;
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
              this::consumeAction, () -> LOG.warn("Cannot get action from {}", event)
          );
    } else {
      LOG.trace("{} is disabled. Ignoring {}", this.getClass().getSimpleName(), event);
    }
  }

  private void consumeAction(ReplicationAction action) {
    ReplicationActionType replicationActionType = action.getType();
    List<SlingUri> slingUris = Stream.of(action.getPaths())
        .map(path -> new DefaultSlingUriBuilder(path, resourceResolverFactory).build())
        .collect(Collectors.toUnmodifiableList());
    Optional.ofNullable(actionsMap.get(replicationActionType))
        .ifPresentOrElse(
            ingestionAction -> ingest(ingestionAction, slingUris),
            () -> LOG.warn("Failed consuming {} for {}", action, slingUris)
        );
  }

  private void ingest(PublicationAction ingestionAction, Collection<SlingUri> slingUris) {
    LOG.trace("Ingesting {} for {}", ingestionAction, slingUris);
    slingUris.stream().map(
        slingUri -> new IngestedData() {
          @Override
          public PublicationAction ingestionAction() {
            return ingestionAction;
          }

          @Override
          public SlingUri uriToIngest() {
            return slingUri;
          }

          @Override
          public Map<String, Object> properties() {
            return Map.of();
          }
        }
    ).forEach(this::ingest);
  }

  private void ingest(IngestedData ingestedData) {
    LOG.trace("Ingesting '{}'", ingestedData.uriToIngest());
    streamxPublicationService.ingest(ingestedData);
  }
}
