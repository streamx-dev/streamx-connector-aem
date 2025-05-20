package dev.streamx.aem.connector.blueprints;

import com.day.cq.dam.api.Asset;
import com.day.cq.dam.api.Rendition;
import dev.streamx.sling.connector.util.SimpleInternalRequest;
import java.io.InputStream;
import java.util.Optional;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.uri.SlingUri;
import org.apache.sling.engine.SlingRequestProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class AssetContent {

  private static final Logger LOG = LoggerFactory.getLogger(AssetContent.class);
  private final SlingUri slingUri;
  private final SlingRequestProcessor slingRequestProcessor;
  private final ResourceResolverFactory resourceResolverFactory;

  AssetContent(
      SlingUri slingUri,
      SlingRequestProcessor slingRequestProcessor,
      ResourceResolverFactory resourceResolverFactory
  ) {
    this.slingUri = slingUri;
    this.slingRequestProcessor = slingRequestProcessor;
    this.resourceResolverFactory = resourceResolverFactory;
  }

  @SuppressWarnings({"squid:S1874", "deprecation"})
  Optional<InputStream> get() {
    LOG.trace("Getting content for '{}'", slingUri);
    try (
        ResourceResolver resourceResolver
            = resourceResolverFactory.getAdministrativeResourceResolver(null)
    ) {
      if (!ResourcePrimaryNodeTypeChecker.isAsset(slingUri, resourceResolverFactory)) {
        return Optional.empty();
      }
      return Optional.of(resourceResolver.resolve(slingUri.toString()))
          .map(assetResource -> assetResource.adaptTo(Asset.class))
          .map(Asset::getOriginal)
          .map(Rendition::getStream)
          .or(
              () -> {
                SimpleInternalRequest simpleInternalRequest
                    = new SimpleInternalRequest(
                    slingUri, slingRequestProcessor, resourceResolverFactory
                );
                return simpleInternalRequest.getResponseAsInputStream();
              }
          );
    } catch (LoginException exception) {
      String message = String.format("Cannot get content for %s", slingUri);
      throw new IllegalStateException(message, exception);
    }
  }
}
