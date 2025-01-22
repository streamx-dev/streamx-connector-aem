package dev.streamx.aem.connector.blueprints;

import dev.streamx.blueprints.data.Asset;
import dev.streamx.sling.connector.PublicationHandler;
import dev.streamx.sling.connector.PublishData;
import dev.streamx.sling.connector.UnpublishData;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.util.Optional;
import org.apache.commons.io.IOUtils;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.engine.SlingRequestProcessor;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(service = PublicationHandler.class)
@Designate(ocd = AssetPublicationHandlerConfig.class)
public class AssetPublicationHandler implements PublicationHandler<Asset> {

  private static final Logger LOG = LoggerFactory.getLogger(AssetPublicationHandler.class);

  private static final String ID = "streamx-asset";
  private static final String CHANNEL = "assets";

  @Reference
  private ResourceResolverFactory resolverFactory;

  @Reference
  private SlingRequestProcessor requestProcessor;

  private boolean enabled;
  private String assetsPathRegexp;

  @Activate
  private void activate(AssetPublicationHandlerConfig config) {
    enabled = config.enabled();
    assetsPathRegexp = config.assets_path_regexp();
  }

  @Override
  public String getId() {
    return ID;
  }

  @Override
  public boolean canHandle(String resourcePath) {
    AssetCandidate assetCandidate = new AssetCandidate(resolverFactory, resourcePath);
    return enabled
        && assetCandidate.isInTree(assetsPathRegexp)
        && assetCandidate.notJCRContent()
        && assetCandidate.isNTDamAsset();
  }

  @Override
  public PublishData<Asset> getPublishData(String resourcePath) {
    try (ResourceResolver resourceResolver = createResourceResolver()) {
      Resource resource = resourceResolver.getResource(resourcePath);

      if (resource == null) {
        LOG.error("Resource not found when trying to publish it: {}", resourcePath);
        return null;
      }

      return new PublishData<>(
          new AssetKey(resolverFactory, resourcePath).asString(),
          CHANNEL,
          Asset.class,
          getAssetModel(resource));
    }
  }

  private ResourceResolver createResourceResolver() {
    try {
      return resolverFactory.getAdministrativeResourceResolver(null);
    } catch (LoginException e) {
      throw new IllegalStateException("Cannot create ResourceResolver", e);
    }
  }

  @Override
  public UnpublishData<Asset> getUnpublishData(String resourcePath) {
    return new UnpublishData<>(
        new AssetKey(resolverFactory, resourcePath).asString(),
        CHANNEL,
        Asset.class
    );
  }

  private Asset getAssetModel(Resource resource) {
    InputStream inputStream = Optional.of(resource)
        .flatMap(
            assetResource -> Optional.ofNullable(assetResource.adaptTo(com.day.cq.dam.api.Asset.class))
        ).map(asset -> new WithFallbackOriginalRendition(resolverFactory, requestProcessor, asset))
        .map(WithFallbackOriginalRendition::originalRendition)
        .map(WithFallbackIS::getStream)
        .orElse(null);
    if (inputStream == null) {
      throw new IllegalStateException(
          "Cannot get InputStream from asset's original rendition: " + resource.getPath());
    }
    try {
      return new Asset(ByteBuffer.wrap(IOUtils.toByteArray(inputStream)));
    } catch (IOException e) {
      throw new UncheckedIOException("Cannot create asset model", e);
    }
  }

}
