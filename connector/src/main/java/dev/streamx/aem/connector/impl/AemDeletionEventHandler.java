package dev.streamx.aem.connector.impl;

import dev.streamx.sling.connector.ResourceToIngest;
import dev.streamx.sling.connector.StreamxPublicationException;
import dev.streamx.sling.connector.StreamxPublicationService;
import java.util.Collections;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.propertytypes.ServiceRanking;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(
    service = EventHandler.class,
    immediate = true,
    property = EventConstants.EVENT_TOPIC + "=com/adobe/cq/resource/delete",
    configurationPolicy = ConfigurationPolicy.OPTIONAL
)
@ServiceRanking(Integer.MAX_VALUE)
public class AemDeletionEventHandler implements EventHandler {

  private static final Logger LOG = LoggerFactory.getLogger(AemDeletionEventHandler.class);

  private final StreamxPublicationService streamxPublicationService;
  private final ResourceResolverFactory resourceResolverFactory;

  @Activate
  public AemDeletionEventHandler(
      @Reference(cardinality = ReferenceCardinality.MANDATORY)
      StreamxPublicationService streamxPublicationService,
      @Reference(cardinality = ReferenceCardinality.MANDATORY)
      ResourceResolverFactory resourceResolverFactory
  ) {
    this.streamxPublicationService = streamxPublicationService;
    this.resourceResolverFactory = resourceResolverFactory;
  }

  @Override
  public void handleEvent(Event event) {
    LOG.trace("Received {}", event);

    if (!streamxPublicationService.isEnabled()) {
      LOG.trace("{} is disabled. Ignoring {}", StreamxPublicationService.class, event);
      return;
    }

    String eventType = (String) event.getProperty("type");
    boolean isPreDelete = "preDelete".equals(eventType);
    if (!isPreDelete) {
      LOG.warn("Unexpected event type: {}. Exiting", eventType);
      return;
    }

    String path = (String) event.getProperty("path");
    String primaryNodeType = PrimaryNodeTypeExtractor.extract(path, resourceResolverFactory);
    ResourceToIngest resource = new ResourceToIngest(path, primaryNodeType);
    handleIngestion(resource);
  }

  private void handleIngestion(ResourceToIngest resource) {
    LOG.trace("Unpublishing {}", resource.getPath());
    try {
      streamxPublicationService.unpublish(Collections.singletonList(resource));
    } catch (StreamxPublicationException exception) {
      LOG.error("Error unpublishing " + resource.getPath(), exception);
    }
  }

}