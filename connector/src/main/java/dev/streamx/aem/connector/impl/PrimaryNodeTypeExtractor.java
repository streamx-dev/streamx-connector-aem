package dev.streamx.aem.connector.impl;

import com.drew.lang.annotations.Nullable;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class PrimaryNodeTypeExtractor {

  private static final Logger LOG = LoggerFactory.getLogger(PrimaryNodeTypeExtractor.class);

  private PrimaryNodeTypeExtractor() {

  }

  @Nullable
  static String extract(String path, ResourceResolverFactory resourceResolverFactory) {
    try (
        @SuppressWarnings("deprecation")
        ResourceResolver resourceResolver = resourceResolverFactory.getAdministrativeResourceResolver(null)
    ) {
      Resource resource = resourceResolver.resolve(path);
      Node node = resource.adaptTo(Node.class);
      if (node != null) {
        return node.getPrimaryNodeType().getName();
      }
    } catch (RepositoryException | LoginException exception) {
      LOG.error("Failed to extract primary node type from {}", path, exception);
    }
    return null;
  }

}