package dev.streamx.aem.connector.impl;

import dev.streamx.sling.connector.StreamxPublicationService;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract class BaseAemEventHandler implements EventHandler {

  private static final Logger LOG = LoggerFactory.getLogger(BaseAemEventHandler.class);

  protected final StreamxPublicationService streamxPublicationService;
  private final ResourceResolverFactory resourceResolverFactory;

  protected BaseAemEventHandler(
      StreamxPublicationService streamxPublicationService,
      ResourceResolverFactory resourceResolverFactory
  ) {
    this.streamxPublicationService = streamxPublicationService;
    this.resourceResolverFactory = resourceResolverFactory;
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

  protected ResourceResolver createResourceResolver() {
    try {
      return resourceResolverFactory.getAdministrativeResourceResolver(null);
    } catch (LoginException e) {
      throw new IllegalStateException("Cannot create ResourceResolver", e);
    }
  }

}