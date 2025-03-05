package dev.streamx.aem.connector.blueprints;

import com.adobe.cq.dam.cfm.ContentFragment;
import java.util.Optional;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class AssetKey {

  private static final Logger LOG = LoggerFactory.getLogger(AssetKey.class);

  private final ResourceResolverFactory rrFactory;
  private final String nodePath;

  AssetKey(ResourceResolverFactory rrFactory, String nodePath) {
    this.rrFactory = rrFactory;
    this.nodePath = nodePath;
  }

  @SuppressWarnings({"squid:S1874", "deprecation"})
  Optional<String> asString() {
    try (ResourceResolver resourceResolver = rrFactory.getAdministrativeResourceResolver(null)) {
      return Optional.ofNullable(resourceResolver.getResource(nodePath))
          .flatMap(resource ->
              Optional.ofNullable(resource.adaptTo(ContentFragment.class))
                  .map(contentFragment -> resource.getPath() + ".json")
                  .or(() -> Optional.of(resource.getPath()))
          );
    } catch (LoginException exception) {
      String message = String.format("Cannot get asset key for %s", nodePath);
      LOG.error(message, exception);
      return Optional.empty();
    }
  }
}
