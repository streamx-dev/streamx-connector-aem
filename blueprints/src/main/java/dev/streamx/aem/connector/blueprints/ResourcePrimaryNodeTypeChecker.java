package dev.streamx.aem.connector.blueprints;

import com.adobe.aem.formsndocuments.util.FMConstants;
import com.day.cq.dam.api.DamConstants;
import com.drew.lang.annotations.NotNull;
import dev.streamx.sling.connector.ResourceInfo;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.uri.SlingUri;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class ResourcePrimaryNodeTypeChecker {

  private static final Logger LOG = LoggerFactory.getLogger(ResourcePrimaryNodeTypeChecker.class);

  private ResourcePrimaryNodeTypeChecker() {
    // no instances
  }

  static boolean isAsset(ResourceInfo resource) {
    return DamConstants.NT_DAM_ASSET.equals(resource.getPrimaryNodeType());
  }

  static boolean isAsset(SlingUri slingUri, ResourceResolver resourceResolver) {
    return hasPrimaryNodeType(slingUri, DamConstants.NT_DAM_ASSET, resourceResolver);
  }

  static boolean isPage(ResourceInfo resource, String requiredPathRegex) {
    boolean isPageNodeType = FMConstants.CQ_PAGE_NODETYPE.equals(resource.getPrimaryNodeType());
    boolean isRequiredPath = resource.getPath().matches(requiredPathRegex);
    boolean isPage = isPageNodeType && isRequiredPath;
    LOG.trace(
        "Is {} a page? Answer: {}. Is NodeType: {}. Is required path: {}",
        resource.getPath(), isPage, isPageNodeType, isRequiredPath
    );
    return isPage;
  }

  static boolean isXF(ResourceInfo resource) {
    boolean isPage = isPage(resource, ".*");
    boolean isXFPath = resource.getPath().startsWith("/content/experience-fragments");
    boolean isXF = isPage && isXFPath;
    LOG.trace(
        "Is {} an XF? Answer: {}. Is Page: {}. Is XF path: {}",
        resource.getPath(), isXF, isPage, isXFPath
    );
    return isXF;
  }

  private static boolean hasPrimaryNodeType(SlingUri slingUri, @NotNull String expectedPrimaryNodeType, ResourceResolver resourceResolver) {
    try {
      Resource resource = resourceResolver.resolve(slingUri.toString());
      Node node = resource.adaptTo(Node.class);
      if (node != null) {
        return node.isNodeType(expectedPrimaryNodeType);
      }
    } catch (RepositoryException exception) {
      LOG.error("Failed to extract primary node type from {}", slingUri, exception);
    }
    return false;
  }
}
