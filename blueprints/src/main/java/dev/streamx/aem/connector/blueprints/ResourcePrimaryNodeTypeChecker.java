package dev.streamx.aem.connector.blueprints;

import com.adobe.aem.formsndocuments.util.FMConstants;
import com.day.cq.dam.api.DamConstants;
import com.drew.lang.annotations.NotNull;
import java.util.Optional;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.uri.SlingUri;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class ResourcePrimaryNodeTypeChecker {

  private static final Logger LOG = LoggerFactory.getLogger(ResourcePrimaryNodeTypeChecker.class);

  private ResourcePrimaryNodeTypeChecker() {
    // no instances
  }

  static boolean isAsset(SlingUri slingUri, ResourceResolverFactory resourceResolverFactory) {
    return hasPrimaryNodeType(slingUri, DamConstants.NT_DAM_ASSET, resourceResolverFactory);
  }

  static boolean isPage(SlingUri slingUri, String requiredPathRegex, ResourceResolverFactory resourceResolverFactory) {
    boolean isPageNodeType = hasPrimaryNodeType(slingUri, FMConstants.CQ_PAGE_NODETYPE, resourceResolverFactory);
    boolean isRequiredPath = slingUri.toString().matches(requiredPathRegex);
    boolean isPage = isPageNodeType && isRequiredPath;
    LOG.trace(
        "Is {} a page? Answer: {}. Is NodeType: {}. Is required path: {}",
        slingUri, isPage, isPageNodeType, isRequiredPath
    );
    return isPage;
  }

  static boolean isXF(SlingUri slingUri, ResourceResolverFactory resourceResolverFactory) {
    boolean isPage = isPage(slingUri, ".*", resourceResolverFactory);
    String resourcePath = Optional.ofNullable(slingUri.getResourcePath()).orElse(StringUtils.EMPTY);
    boolean isXFPath = resourcePath.startsWith("/content/experience-fragments");
    boolean isXF = isPage && isXFPath;
    LOG.trace(
        "Is {} an XF? Answer: {}. Is Page: {}. Is XF path: {}",
        slingUri, isXF, isPage, isXFPath
    );
    return isXF;
  }

  private static boolean hasPrimaryNodeType(SlingUri slingUri, @NotNull String expectedPrimaryNodeType, ResourceResolverFactory resourceResolverFactory) {
    try (
        @SuppressWarnings("deprecation")
        ResourceResolver resourceResolver = resourceResolverFactory.getAdministrativeResourceResolver(null)
    ) {
      Resource resource = resourceResolver.resolve(slingUri.toString());
      Node node = resource.adaptTo(Node.class);
      if (node != null) {
        return node.isNodeType(expectedPrimaryNodeType);
      }
    } catch (RepositoryException | LoginException exception) {
      LOG.error("Failed to extract primary node type from {}", slingUri, exception);
    }
    return false;
  }
}
