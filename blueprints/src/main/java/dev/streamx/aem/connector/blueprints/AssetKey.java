package dev.streamx.aem.connector.blueprints;

import com.adobe.cq.dam.cfm.ContentFragment;
import java.util.Optional;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;

class AssetKey {

  private final ResourceResolverFactory rrFactory;
  private final String nodePath;

  AssetKey(ResourceResolverFactory rrFactory, String nodePath) {
    this.rrFactory = rrFactory;
    this.nodePath = nodePath;
  }

  @SneakyThrows
  String asString() {
    try (ResourceResolver resourceResolver = rrFactory.getAdministrativeResourceResolver(null)) {
      return Optional.ofNullable(resourceResolver.getResource(nodePath))
          .filter( resource -> Optional.ofNullable(resource.adaptTo(ContentFragment.class)).isPresent())
          .map(Resource::getPath)
          .map(path -> String.format("%s.json", path))
          .or(
              () -> Optional.ofNullable(resourceResolver.getResource(nodePath))
                  .map(Resource::getPath)
          ).orElse(StringUtils.EMPTY);
    }
  }
}
