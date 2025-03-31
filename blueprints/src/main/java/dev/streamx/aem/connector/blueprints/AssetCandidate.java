package dev.streamx.aem.connector.blueprints;

import com.day.cq.dam.api.DamConstants;
import dev.streamx.sling.connector.IngestedData;
import java.util.Map;
import java.util.Optional;
import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.resource.ResourceResolverFactory;

class AssetCandidate {

  private final NodeTypeCheck nodeTypeCheck;
  private final IngestedData ingestedData;

  AssetCandidate(ResourceResolverFactory resourceResolverFactory, IngestedData ingestedData) {
    this.ingestedData = ingestedData;
    this.nodeTypeCheck = new NodeTypeCheck(resourceResolverFactory, ingestedData.uriToIngest());
  }

  boolean isAsset() {
    boolean primaryNTFromJCRMatches = nodeTypeCheck.matches(DamConstants.NT_DAM_ASSET);
    if (primaryNTFromJCRMatches) {
      return true;
    }
    Map<String, Object> idProps = ingestedData.properties();
    return Optional.ofNullable(
            idProps.get(String.format("streamx.%s", JcrConstants.JCR_PRIMARYTYPE))
        ).filter(String.class::isInstance)
        .map(String.class::cast)
        .filter(DamConstants.NT_DAM_ASSET::equals)
        .isPresent();
  }
}
