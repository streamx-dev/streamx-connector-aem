package dev.streamx.aem.connector.blueprints;

import com.day.cq.dam.api.DamConstants;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.uri.SlingUri;

class AssetCandidate {

  private final NodeTypeCheck nodeTypeCheck;

  AssetCandidate(ResourceResolverFactory resourceResolverFactory, SlingUri slingUri) {
    this.nodeTypeCheck = new NodeTypeCheck(resourceResolverFactory, slingUri);
  }

  boolean isAsset() {
    return nodeTypeCheck.matches(DamConstants.NT_DAM_ASSET);
  }
}
