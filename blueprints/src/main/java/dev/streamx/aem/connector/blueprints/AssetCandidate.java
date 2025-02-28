package dev.streamx.aem.connector.blueprints;

import com.day.cq.dam.api.DamConstants;
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

class AssetCandidate {

  private static final Logger LOG = LoggerFactory.getLogger(AssetCandidate.class);
  private final SlingUri slingUri;
  private final ResourceResolverFactory resourceResolverFactory;

  AssetCandidate(ResourceResolverFactory resourceResolverFactory, SlingUri slingUri) {
    this.resourceResolverFactory = resourceResolverFactory;
    this.slingUri = slingUri;
  }

  @SuppressWarnings({"squid:S1874", "deprecation"})
  boolean isAsset() {
    try (
        ResourceResolver resourceResolver
            = resourceResolverFactory.getAdministrativeResourceResolver(null)
    ) {
      Resource resource = resourceResolver.resolve(slingUri.toString());
      boolean isAsset = Optional.ofNullable(resource.adaptTo(Node.class))
          .map(this::extractPrimaryNt)
          .stream()
          .anyMatch(primaryNT -> primaryNT.equals(DamConstants.NT_DAM_ASSET));
      LOG.trace("Is '{}' an asset? Answer: {}", slingUri, isAsset);
      return isAsset;
    } catch (LoginException exception) {
      String message = String.format("Can't determine if '%s' is an asset", slingUri);
      LOG.error(message, exception);
      return false;
    }
  }


  private String extractPrimaryNt(Node node) {
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
