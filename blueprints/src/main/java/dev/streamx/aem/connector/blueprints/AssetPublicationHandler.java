package dev.streamx.aem.connector.blueprints;

import com.day.cq.dam.api.Rendition;
import dev.streamx.blueprints.data.Asset;
import dev.streamx.sling.connector.PublicationHandler;
import dev.streamx.sling.connector.PublishData;
import dev.streamx.sling.connector.ResourceInfo;
import dev.streamx.sling.connector.UnpublishData;
import dev.streamx.sling.connector.util.SimpleInternalRequest;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.uri.SlingUri;
import org.apache.sling.api.uri.SlingUriBuilder;
import org.apache.sling.engine.SlingRequestProcessor;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.propertytypes.ServiceDescription;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(service = PublicationHandler.class)
@Designate(ocd = AssetPublicationHandlerConfig.class)
@ServiceDescription("Publication handler for AEM Assets")
public class AssetPublicationHandler extends BasePublicationHandler<Asset> {

  private static final Logger LOG = LoggerFactory.getLogger(AssetPublicationHandler.class);

  private final SlingRequestProcessor slingRequestProcessor;
  private final AtomicReference<AssetPublicationHandlerConfig> config;

  @Activate
  public AssetPublicationHandler(
      @Reference(cardinality = ReferenceCardinality.MANDATORY)
      ResourceResolverFactory resolverFactory,
      @Reference(cardinality = ReferenceCardinality.MANDATORY)
      SlingRequestProcessor slingRequestProcessor,
      AssetPublicationHandlerConfig config
  ) {
    super(resolverFactory);
    this.slingRequestProcessor = slingRequestProcessor;
    this.config = new AtomicReference<>(config);
  }

  @Modified
  void configure(AssetPublicationHandlerConfig config) {
    this.config.set(config);
  }

  @Override
  public boolean canHandle(ResourceInfo resource) {
    if (!config.get().enabled()) {
      return false;
    }

    String resourcePath = resource.getPath();
    if (!resourcePath.matches(config.get().assets_path_regexp())) {
      return false;
    }

    try (ResourceResolver resourceResolver = createResourceResolver()) {
      return ResourcePrimaryNodeTypeChecker.isAsset(resource, resourceResolver);
    }
  }

  @Override
  public PublishData<Asset> getPublishData(String resourcePath) {
    LOG.trace("Generating publish data for '{}'", resourcePath);
    try (ResourceResolver resourceResolver = createResourceResolver()) {
      SlingUri slingUri = SlingUriBuilder.parse(resourcePath, resourceResolver).build();
      Resource resource = resourceResolver.resolve(
          Optional.ofNullable(slingUri.getPath()).orElse(StringUtils.EMPTY)
      );

      if (ResourceUtil.isNonExistingResource(resource)) {
        LOG.error("Resource not found for publish data generation: {}", slingUri);
        return null;
      }

      if (!ResourcePrimaryNodeTypeChecker.isAsset(slingUri, resourceResolver)) {
        LOG.error("Not an Asset for publish data generation: {}", slingUri);
        return null;
      }

      Map<String, String> messageProps = getSxTypeAsMap(resource, config.get().jcr_prop_name_for_sx_type());
      return generateAssetModel(slingUri, resourceResolver)
          .map(asset -> generatePublishData(slingUri, messageProps, asset))
          .orElse(null);
    }
  }

  private PublishData<Asset> generatePublishData(SlingUri slingUri, Map<String, String> messageProps, Asset asset) {
    return new PublishData<>(
        slingUri.toString(),
        config.get().publication_channel(),
        Asset.class,
        asset,
        messageProps
    );
  }

  @Override
  public UnpublishData<Asset> getUnpublishData(String resourcePath) {
    try (ResourceResolver resourceResolver = createResourceResolver()) {
      AssetHashManager.deleteAssetHash(resourcePath, resourceResolver);
    }

    return new UnpublishData<>(
        resourcePath, config.get().publication_channel(), Asset.class
    );
  }

  private Optional<Asset> generateAssetModel(SlingUri slingUri, ResourceResolver resourceResolver) {
    LOG.trace("Getting {} content for '{}'", Asset.class, slingUri);
    return Optional.of(resourceResolver.resolve(slingUri.toString()))
        .map(assetResource -> assetResource.adaptTo(com.day.cq.dam.api.Asset.class))
        .map(com.day.cq.dam.api.Asset::getOriginal)
        .map(Rendition::getStream)
        .or(() -> new SimpleInternalRequest(slingUri, slingRequestProcessor, resourceResolver).getResponseAsInputStream())
        .map(inputStream -> new Asset(InputStreamConverter.toByteBuffer(inputStream)))
        .flatMap(asset -> assetIfContentChanged(asset, slingUri, resourceResolver));
  }

  private static Optional<Asset> assetIfContentChanged(Asset asset, SlingUri slingUri, ResourceResolver resourceResolver) {
    try {
      if (AssetHashManager.hasAssetContentChanged(slingUri.getResourcePath(), asset.getContent().array(), resourceResolver)) {
        return Optional.of(asset);
      } else {
        LOG.trace("No changes in the content of Asset '{}'", slingUri);
        return Optional.empty();
      }
    } catch (Exception ex) {
      LOG.error("Error verifying if content of Asset '{}' has changed", slingUri, ex);
      return Optional.of(asset);
    }
  }

}
