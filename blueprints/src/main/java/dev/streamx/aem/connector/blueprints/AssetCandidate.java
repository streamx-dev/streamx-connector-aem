package dev.streamx.aem.connector.blueprints;

import com.day.cq.commons.DownloadResource;
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
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.uri.SlingUri;
import org.apache.sling.api.uri.SlingUriBuilder;
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

  boolean isAsset() {
    boolean isAsset = isUsualAsset(slingUri) || isReferencingAsset(slingUri);
    LOG.trace("Is '{}' an asset? Answer: {}", slingUri, isAsset);
    return isAsset;
  }

  @SuppressWarnings({"squid:S1874", "deprecation"})
  private boolean isUsualAsset(SlingUri slingUri) {
    try (
        ResourceResolver resourceResolver
            = resourceResolverFactory.getAdministrativeResourceResolver(null)
    ) {
      Resource resource = resourceResolver.resolve(slingUri.toString());
      boolean isUsualAsset = Optional.ofNullable(resource.adaptTo(Node.class))
          .map(this::extractPrimaryNT)
          .stream()
          .anyMatch(primaryNT -> primaryNT.equals(DamConstants.NT_DAM_ASSET));
      LOG.trace("Is '{}' a usual asset? Answer: {}", slingUri, isUsualAsset);
      return isUsualAsset;
    } catch (LoginException exception) {
      String message = String.format("Can't determine if '%s' is usual asset", slingUri);
      LOG.error(message, exception);
      return false;
    }
  }


  private String extractPrimaryNT(Node node) {
    try {
      NodeType primaryNT = node.getPrimaryNodeType();
      return primaryNT.getName();
    } catch (RepositoryException exception) {
      String message = String.format("Failed to extract primary node type for '%s'", node);
      LOG.error(message, exception);
      return StringUtils.EMPTY;
    }
  }

  @SuppressWarnings({"squid:S1874", "deprecation"})
  private Optional<SlingUri> referencedAssetUri(SlingUri wrappingSlingUri) {
    try (
        ResourceResolver resourceResolver
            = resourceResolverFactory.getAdministrativeResourceResolver(null)
    ) {
      Resource wrappingResource = resourceResolver.resolve(
          Optional.ofNullable(wrappingSlingUri.getPath()).orElse(StringUtils.EMPTY)
      );
      LOG.trace("For {} wrapping resource resolved: {}", wrappingSlingUri, wrappingResource);
      Optional<SlingUri> referencedAssetUri = Optional.ofNullable(
              wrappingResource.adaptTo(ValueMap.class)
          ).map(valueMap -> valueMap.get(DownloadResource.PN_REFERENCE, String.class))
          .map(fileReference -> SlingUriBuilder.parse(fileReference, resourceResolver).build())
          .filter(this::isUsualAsset);
      LOG.trace("Referenced asset uri for '{}': '{}'", wrappingSlingUri, referencedAssetUri);
      return referencedAssetUri;
    } catch (LoginException exception) {
      String message = String.format(
          "Can't determine the referenced asset uri for '%s'", wrappingSlingUri
      );
      LOG.error(message, exception);
      return Optional.empty();
    }
  }

  private boolean isReferencingAsset(SlingUri slingUri) {
    boolean isReferencingAsset = referencedAssetUri(slingUri).isPresent();
    LOG.trace(
        "Does the resource at '{}' reference asset? Answer: {}", slingUri, isReferencingAsset
    );
    return isReferencingAsset;
  }
}
