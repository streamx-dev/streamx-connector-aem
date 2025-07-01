package dev.streamx.aem.connector.impl;

import dev.streamx.sling.connector.ResourceInfo;
import dev.streamx.sling.connector.StreamxPublicationException;
import dev.streamx.sling.connector.StreamxPublicationService;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract class BaseAemEventHandler implements EventHandler {

  private static final Logger LOG = LoggerFactory.getLogger(BaseAemEventHandler.class);

  private final StreamxPublicationService streamxPublicationService;
  private final ResourceResolverFactory resourceResolverFactory;
  private final AtomicReference<Set<String>> propertiesToLoadFromJcr = new AtomicReference<>();

  protected BaseAemEventHandler(
      StreamxPublicationService streamxPublicationService,
      ResourceResolverFactory resourceResolverFactory,
      String[] propertiesToLoadFromJcr
  ) {
    this.streamxPublicationService = streamxPublicationService;
    this.resourceResolverFactory = resourceResolverFactory;
    configure(propertiesToLoadFromJcr);
  }

  protected void configure(String[] propertiesToLoadFromJcr) {
    this.propertiesToLoadFromJcr.set(
        Optional.ofNullable(propertiesToLoadFromJcr)
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

  protected Map<String, String> readJcrProperties(String path, ResourceResolver resourceResolver) {
    Map<String, String> result = new LinkedHashMap<>();
    Resource resource = resourceResolver.getResource(path);
    if (resource != null) {
      Node node = resource.adaptTo(Node.class);
      if (node != null) {
        for (String propertyName : propertiesToLoadFromJcr.get()) {
          result.put(propertyName, getPropertyValue(node, propertyName));
        }
      }
    }
    return result;
  }

  private static String getPropertyValue(Node node, String propertyName) {
    try {
      if (node.hasProperty(propertyName)) {
        Property property = node.getProperty(propertyName);
        if (property.isMultiple()) {
          LOG.warn("Multivalued properties are not handled: {}", propertyName);
        } else {
          return property.getValue().getString();
        }
      }
    } catch (RepositoryException ex) {
      LOG.error("Error reading property {}", propertyName, ex);
    }
    return null;
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
    try {
      streamxPublicationService.publish(resources);
    } catch (StreamxPublicationException exception) {
      LOG.error("Failed to handle publish for {}", resources, exception);
    }
  }

  protected void unpublish(List<ResourceInfo> resources) {
    LOG.trace("Unpublishing {}", resources);
    try {
      streamxPublicationService.unpublish(resources);
    } catch (StreamxPublicationException exception) {
      LOG.error("Failed to handle unpublish for {}", resources, exception);
    }
  }
}