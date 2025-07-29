package dev.streamx.aem.connector.impl;

import dev.streamx.sling.connector.ResourceInfo;
import dev.streamx.sling.connector.StreamxPublicationService;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.ValueMap;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract class BaseAemEventHandler implements EventHandler {

  private static final Logger LOG = LoggerFactory.getLogger(BaseAemEventHandler.class);

  private final StreamxPublicationService streamxPublicationService;
  private final ResourceResolverFactory resourceResolverFactory;
  private final AtomicReference<Set<String>> resourcePropertiesToLoad = new AtomicReference<>();

  protected BaseAemEventHandler(
      StreamxPublicationService streamxPublicationService,
      ResourceResolverFactory resourceResolverFactory,
      String[] resourcePropertiesToLoad
  ) {
    this.streamxPublicationService = streamxPublicationService;
    this.resourceResolverFactory = resourceResolverFactory;
    configure(resourcePropertiesToLoad);
  }

  protected void configure(String[] resourcePropertiesToLoad) {
    this.resourcePropertiesToLoad.set(
        Optional.ofNullable(resourcePropertiesToLoad)
            .map(Set::of)
            .orElseGet(Collections::emptySet)
    );
  }

  @Override
  public final void handleEvent(Event event) {
    LOG.trace("Received {}", event);

    if (!streamxPublicationService.isEnabled()) {
      LOG.trace("{} is disabled. Ignoring {}", StreamxPublicationService.class, event);
      return;
    }

    doHandleEvent(event);
  }

  protected abstract void doHandleEvent(Event event);

  protected Map<String, String> loadResourceProperties(String path, ResourceResolver resourceResolver) {
    Map<String, String> result = new LinkedHashMap<>();
    Resource resource = resourceResolver.getResource(path);
    if (resource != null) {
      ValueMap valueMap = resource.getValueMap();
      for (String propertyName : resourcePropertiesToLoad.get()) {
        Object propertyValue = valueMap.get(propertyName);
        if (propertyValue == null) {
          result.put(propertyName, null);
        } else if (!propertyValue.getClass().isArray()) {
          result.put(propertyName, String.valueOf(propertyValue));
        }
      }
    }
    return result;
  }

  protected ResourceResolver createResourceResolver() {
    try {
      return resourceResolverFactory.getAdministrativeResourceResolver(null);
    } catch (LoginException e) {
      throw new IllegalStateException("Cannot create ResourceResolver", e);
    }
  }

  protected void publish(List<ResourceInfo> resources) {
    LOG.trace("Publishing {}", resources);
    streamxPublicationService.publish(resources);
  }

  protected void unpublish(List<ResourceInfo> resources) {
    LOG.trace("Unpublishing {}", resources);
    streamxPublicationService.unpublish(resources);
  }
}