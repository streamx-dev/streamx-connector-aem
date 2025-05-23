package dev.streamx.aem.connector.blueprints;

import com.day.cq.dam.api.Asset;
import com.day.cq.dam.api.Rendition;
import dev.streamx.sling.connector.util.SimpleInternalRequest;
import java.io.InputStream;
import java.util.Optional;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.uri.SlingUri;
import org.apache.sling.engine.SlingRequestProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class AssetContent {

  private static final Logger LOG = LoggerFactory.getLogger(AssetContent.class);

  private AssetContent() {
    // no instances
  }

  static Optional<InputStream> get(SlingUri slingUri, SlingRequestProcessor slingRequestProcessor, ResourceResolver resourceResolver) {
    LOG.trace("Getting content for '{}'", slingUri);
    if (!ResourcePrimaryNodeTypeChecker.isAsset(slingUri, resourceResolver)) {
      return Optional.empty();
    }
    return Optional.of(resourceResolver.resolve(slingUri.toString()))
        .map(assetResource -> assetResource.adaptTo(Asset.class))
        .map(Asset::getOriginal)
        .map(Rendition::getStream)
        .or(() -> new SimpleInternalRequest(slingUri, slingRequestProcessor, resourceResolver).getResponseAsInputStream());
  }
}
