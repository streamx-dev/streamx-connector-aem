package dev.streamx.aem.connector.blueprints;

import java.util.Optional;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeType;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.uri.SlingUri;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class NodeTypeCheck {

  private static final Logger LOG = LoggerFactory.getLogger(NodeTypeCheck.class);

  private NodeTypeCheck() {
    // no instances
  }

  @SuppressWarnings({"squid:S1874", "deprecation"})
  static boolean matches(SlingUri slingUri, String expectedPrimaryNodeType, ResourceResolverFactory resourceResolverFactory) {
    try (
        ResourceResolver resourceResolver
            = resourceResolverFactory.getAdministrativeResourceResolver(null)
    ) {
      Resource resource = resourceResolver.resolve(slingUri.toString());
      boolean matches = Optional.ofNullable(resource.adaptTo(Node.class))
          .map(NodeTypeCheck::extractPrimaryNt)
          .stream()
          .anyMatch(primaryNT -> primaryNT.equals(expectedPrimaryNodeType));
      LOG.trace(
          "Does {} have this node type: '{}'? Answer: {}", slingUri, expectedPrimaryNodeType, matches
      );
      return matches;
    } catch (LoginException exception) {
      String message = String.format(
          "Can't check if %s is of this node type: %s", slingUri, expectedPrimaryNodeType
      );
      LOG.error(message, exception);
      return false;
    }
  }

  private static String extractPrimaryNt(Node node) {
    try {
      NodeType primaryNT = node.getPrimaryNodeType();
      return primaryNT.getName();
    } catch (RepositoryException exception) {
      String message = String.format("Failed to extract primary node type for '%s'", node);
      LOG.error(message, exception);
      return StringUtils.EMPTY;
    }
  }
}
