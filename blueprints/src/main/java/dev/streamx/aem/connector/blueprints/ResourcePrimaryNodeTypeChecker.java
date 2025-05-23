package dev.streamx.aem.connector.blueprints;

import com.day.cq.dam.api.DamConstants;
import com.day.cq.wcm.api.NameConstants;
import com.drew.lang.annotations.NotNull;
import dev.streamx.sling.connector.ResourceInfo;
import javax.jcr.Node;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeManager;
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

  static boolean isAsset(ResourceInfo resource, ResourceResolver resourceResolver) {
    return hasPrimaryNodeType(resource, DamConstants.NT_DAM_ASSET, resourceResolver);
  }

  static boolean isAsset(SlingUri slingUri, ResourceResolver resourceResolver) {
    return hasPrimaryNodeType(slingUri, DamConstants.NT_DAM_ASSET, resourceResolver);
  }

  static boolean isPage(ResourceInfo resource, String requiredPathRegex, ResourceResolver resourceResolver) {
    boolean isPageNodeType = hasPrimaryNodeType(resource, NameConstants.NT_PAGE, resourceResolver);
    boolean isRequiredPath = resource.getPath().matches(requiredPathRegex);
    boolean isPage = isPageNodeType && isRequiredPath;
    LOG.trace(
        "Is {} a page? Answer: {}. Is NodeType: {}. Is required path: {}",
        resource.getPath(), isPage, isPageNodeType, isRequiredPath
    );
    return isPage;
  }

  static boolean isXF(ResourceInfo resource, ResourceResolver resourceResolver) {
    boolean isPage = isPage(resource, ".*", resourceResolver);
    boolean isXFPath = resource.getPath().startsWith("/content/experience-fragments");
    boolean isXF = isPage && isXFPath;
    LOG.trace(
        "Is {} an XF? Answer: {}. Is Page: {}. Is XF path: {}",
        resource.getPath(), isXF, isPage, isXFPath
    );
    return isXF;
  }

  private static boolean hasPrimaryNodeType(ResourceInfo resourceInfo, @NotNull String expectedPrimaryNodeType, ResourceResolver resourceResolver) {
    try {
      Session session = resourceResolver.adaptTo(Session.class);
      if (session == null) {
        LOG.error("Failed to get session to verify if {} is a {}", resourceInfo, expectedPrimaryNodeType);
        return false;
      }
      NodeTypeManager nodeTypeManager = session.getWorkspace().getNodeTypeManager();
      NodeType nodeType = nodeTypeManager.getNodeType(expectedPrimaryNodeType);
      return nodeType.isNodeType(resourceInfo.getPrimaryNodeType());
    } catch (Exception exception) {
      LOG.error("Failed to verify if {} is a {}", resourceInfo, expectedPrimaryNodeType);
      return false;
    }
  }

  private static boolean hasPrimaryNodeType(SlingUri slingUri, @NotNull String expectedPrimaryNodeType, ResourceResolver resourceResolver) {
    try {
      Resource resource = resourceResolver.resolve(slingUri.toString());
      Node node = resource.adaptTo(Node.class);
      if (node == null) {
        LOG.error("Failed to adapt to Node to verify if {} is a {}", slingUri, expectedPrimaryNodeType);
        return false;
      }
      return node.isNodeType(expectedPrimaryNodeType);
    } catch (Exception exception) {
      LOG.error("Failed to verify if {} is a {}", slingUri, expectedPrimaryNodeType);
      return false;
    }
  }
}
