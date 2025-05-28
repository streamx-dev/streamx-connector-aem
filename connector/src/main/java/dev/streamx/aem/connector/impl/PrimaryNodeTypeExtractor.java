package dev.streamx.aem.connector.impl;

import com.drew.lang.annotations.Nullable;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class PrimaryNodeTypeExtractor {

  private static final Logger LOG = LoggerFactory.getLogger(PrimaryNodeTypeExtractor.class);

  private PrimaryNodeTypeExtractor() {
    // no instances
  }

  @Nullable
  static String extract(String path, ResourceResolver resourceResolver) {
    try {
      Resource resource = resourceResolver.resolve(path);
      Node node = resource.adaptTo(Node.class);
      if (node != null) {
        return node.getPrimaryNodeType().getName();
      }
    } catch (RepositoryException exception) {
      LOG.error("Failed to extract primary node type from {}", path, exception);
    }
    return null;
  }

}