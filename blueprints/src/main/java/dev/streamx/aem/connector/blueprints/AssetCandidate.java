package dev.streamx.aem.connector.blueprints;

import com.day.cq.dam.api.DamConstants;
import eu.ciechanowiec.sneakyfun.SneakyFunction;
import java.util.Optional;
import javax.jcr.Node;
import javax.jcr.nodetype.NodeType;
import lombok.SneakyThrows;
import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;

class AssetCandidate {

  private final ResourceResolverFactory rrFactory;
  private final String nodePath;

  AssetCandidate(ResourceResolverFactory rrFactory, String nodePath) {
    this.rrFactory = rrFactory;
    this.nodePath = nodePath;
  }

  @SneakyThrows
  @SuppressWarnings({"squid:S1874", "deprecation"})
  boolean isNTDamAsset() {
    try (ResourceResolver resourceResolver = rrFactory.getAdministrativeResourceResolver(null)) {
      return Optional.ofNullable(resourceResolver.getResource(nodePath))
          .flatMap(resource -> Optional.ofNullable(resource.adaptTo(Node.class)))
          .map(SneakyFunction.sneaky(Node::getPrimaryNodeType))
          .map(SneakyFunction.sneaky(NodeType::getName))
          .map(actualPrimaryType -> actualPrimaryType.equals(DamConstants.NT_DAM_ASSET))
          .orElse(false);
    }
  }

  boolean isInTree(String expectedNodePathRegex) {
    return nodePath.matches(expectedNodePathRegex);
  }

  boolean notJCRContent() {
    return !nodePath.endsWith(JcrConstants.JCR_CONTENT);
  }
}
