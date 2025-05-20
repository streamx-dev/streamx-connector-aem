package dev.streamx.aem.connector.blueprints;

import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.uri.SlingUri;
import org.apache.sling.api.uri.SlingUriBuilder;

final class DefaultSlingUriBuilder {

  private DefaultSlingUriBuilder() {
    // no instances
  }

  static SlingUri build(String rawSlingUri, ResourceResolverFactory resourceResolverFactory) {
    try (
        @SuppressWarnings({"squid:1874", "deprecation"})
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
