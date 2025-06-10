package dev.streamx.aem.connector.impl;

import dev.streamx.sling.connector.ResourceInfo;
import dev.streamx.sling.connector.StreamxPublicationException;
import dev.streamx.sling.connector.StreamxPublicationService;
import java.util.Collections;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
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
public class AemDeletionEventHandler extends BaseAemEventHandler {

  private static final Logger LOG = LoggerFactory.getLogger(AemDeletionEventHandler.class);

  @Activate
  public AemDeletionEventHandler(
      @Reference StreamxPublicationService streamxPublicationService,
      @Reference ResourceResolverFactory resourceResolverFactory
  ) {
    super(streamxPublicationService, resourceResolverFactory);
  }

  @Override
  protected void doHandleEvent(Event event) {
    String eventType = (String) event.getProperty("type");
    boolean isPreDelete = "preDelete".equals(eventType);
    if (!isPreDelete) {
      LOG.warn("Unexpected event type: {}. Exiting", eventType);
      return;
    }

    String path = (String) event.getProperty("path");
    String primaryNodeType = readPrimaryNodeType(path);
    ResourceInfo resource = new ResourceInfo(path, primaryNodeType);
    handleIngestion(resource);
  }

  private String readPrimaryNodeType(String path) {
    try (ResourceResolver resourceResolver = createResourceResolver()) {
      return PrimaryNodeTypeExtractor.extract(path, resourceResolver);
    }
  }

  private void handleIngestion(ResourceInfo resource) {
    LOG.trace("Unpublishing {}", resource.getPath());
    try {
      streamxPublicationService.unpublish(Collections.singletonList(resource));
    } catch (StreamxPublicationException exception) {
      LOG.error("Error unpublishing " + resource.getPath(), exception);
    }
  }

}