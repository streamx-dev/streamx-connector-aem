package dev.streamx.aem.connector.blueprints;

import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.uri.SlingUri;
import org.apache.sling.api.uri.SlingUriBuilder;

class DefaultSlingUriBuilder {

  private final String rawSlingUri;
  private final ResourceResolverFactory resourceResolverFactory;

  DefaultSlingUriBuilder(String rawSlingUri, ResourceResolverFactory resourceResolverFactory) {
    this.rawSlingUri = rawSlingUri;
    this.resourceResolverFactory = resourceResolverFactory;
  }

  @SuppressWarnings({"squid:1874", "deprecation"})
  SlingUri build() {
    try (
        ResourceResolver resourceResolver
            = resourceResolverFactory.getAdministrativeResourceResolver(null)
    ) {
      return SlingUriBuilder.parse(rawSlingUri, resourceResolver).build();
    } catch (LoginException exception) {
      String message = String.format("Unable to build Sling URI from %s", rawSlingUri);
      throw new IllegalArgumentException(message, exception);
    }
  }
}
