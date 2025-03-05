package dev.streamx.aem.connector.blueprints;

import com.day.cq.dam.api.DamConstants;
import java.util.Optional;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeType;
import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class AssetCandidate {

  private static final Logger LOG = LoggerFactory.getLogger(AssetCandidate.class);

  private final ResourceResolverFactory rrFactory;
  private final String nodePath;

  AssetCandidate(ResourceResolverFactory rrFactory, String nodePath) {
    this.rrFactory = rrFactory;
    this.nodePath = nodePath;
  }

  @SuppressWarnings({"squid:S1874", "deprecation"})
  boolean isNTDamAsset() {
    try (ResourceResolver resourceResolver = rrFactory.getAdministrativeResourceResolver(null)) {
      return Optional.ofNullable(resourceResolver.getResource(nodePath))
          .flatMap(resource -> Optional.ofNullable(resource.adaptTo(Node.class)))
          .flatMap(
              node -> {
                try {
                  return Optional.of(node.getPrimaryNodeType());
                } catch (RepositoryException exception) {
                  String message = String.format("Cannot get primary node type of '%s'", nodePath);
                  LOG.error(message, exception);
                  return Optional.empty();
                }
              }
          )
          .map(NodeType::getName)
          .map(actualPrimaryType -> actualPrimaryType.equals(DamConstants.NT_DAM_ASSET))
          .orElse(false);
    } catch (LoginException exception) {
      String message = String.format(
          "Cannot check if '%s' is of type '%s'", nodePath, DamConstants.NT_DAM_ASSET
      );
      LOG.error(message, exception);
      return false;
    }
  }

  boolean isInTree(String expectedNodePathRegex) {
    return nodePath.matches(expectedNodePathRegex);
  }

  boolean notJCRContent() {
    return !nodePath.endsWith(JcrConstants.JCR_CONTENT);
  }
}
