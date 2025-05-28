package dev.streamx.aem.connector.blueprints;

import dev.streamx.sling.connector.PublicationHandler;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;

abstract class BasePublicationHandler<T> implements PublicationHandler<T> {

  protected final ResourceResolverFactory resolverFactory;

  BasePublicationHandler(ResourceResolverFactory resolverFactory) {
    this.resolverFactory = resolverFactory;
  }

  @Override
  public final String getId() {
    return this.getClass().getSimpleName();
  }

  protected ResourceResolver createResourceResolver() {
    try {
      return resolverFactory.getAdministrativeResourceResolver(null);
    } catch (LoginException e) {
      throw new IllegalStateException("Cannot create ResourceResolver", e);
    }
  }
}
