package dev.streamx.aem.connector.impl;

import dev.streamx.sling.connector.ResourceInfo;
import dev.streamx.sling.connector.StreamxPublicationService;
import java.util.Collections;
import java.util.Map;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.propertytypes.ServiceRanking;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(
    service = EventHandler.class,
    immediate = true,
    property = EventConstants.EVENT_TOPIC + "=com/adobe/cq/resource/delete",
    configurationPolicy = ConfigurationPolicy.OPTIONAL
)
@ServiceRanking(Integer.MAX_VALUE)
@Designate(ocd = AemDeletionEventHandlerConfig.class)
public class AemDeletionEventHandler extends BaseAemEventHandler {

  private static final Logger LOG = LoggerFactory.getLogger(AemDeletionEventHandler.class);

  @Activate
  public AemDeletionEventHandler(
      @Reference StreamxPublicationService streamxPublicationService,
      @Reference ResourceResolverFactory resourceResolverFactory,
      AemDeletionEventHandlerConfig config
  ) {
    super(streamxPublicationService, resourceResolverFactory, config.resource_properties_to_load());
  }

  @Modified
  void configure(AemDeletionEventHandlerConfig config) {
    super.configure(config.resource_properties_to_load());
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
    Map<String, String> properties = loadResourceProperties(path);
    ResourceInfo resource = new ResourceInfo(path, properties);
    unpublish(Collections.singletonList(resource));
  }

  private Map<String, String> loadResourceProperties(String path) {
    try (ResourceResolver resourceResolver = createResourceResolver()) {
      return loadResourceProperties(path, resourceResolver);
    }
  }
}