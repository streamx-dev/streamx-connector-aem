package dev.streamx.aem.connector.blueprints;

import dev.streamx.blueprints.data.Asset;
import dev.streamx.sling.connector.PublicationHandler;
import dev.streamx.sling.connector.PublishData;
import dev.streamx.sling.connector.ResourceInfo;
import dev.streamx.sling.connector.UnpublishData;
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
    String resourcePath = resource.getPath();
    try (ResourceResolver resourceResolver = createResourceResolver()) {
      boolean canHandle = config.get().enabled()
          && resourcePath.matches(config.get().assets_path_regexp())
          && ResourcePrimaryNodeTypeChecker.isAsset(resource, resourceResolver);
      LOG.trace("Can handle this resource path: '{}'? Answer: {}", resourcePath, canHandle);
      return canHandle;
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

      Map<String, String> messageProps = getSxTypeAsMap(resource, config.get().jcr_prop_name_for_sx_type());
      return generatePublishData(slingUri, messageProps, resourceResolver);
    }
  }

  private PublishData<Asset> generatePublishData(
      SlingUri slingUri,
      Map<String, String> messageProps,
      ResourceResolver resourceResolver
  ) {
    LOG.trace("Generating publish data for '{}'", slingUri);
    Asset asset = generateAssetModel(slingUri, resourceResolver);
    return new PublishData<>(
        slingUri.toString(), config.get().publication_channel(), Asset.class,
        asset, messageProps
    );
  }

  @Override
  public UnpublishData<Asset> getUnpublishData(String resourcePath) {
    return new UnpublishData<>(
        resourcePath, config.get().publication_channel(), Asset.class
    );
  }

  private Asset generateAssetModel(SlingUri slingUri, ResourceResolver resourceResolver) {
    LOG.trace("Generating {} out of {}", Asset.class, slingUri);
    return AssetContent.get(slingUri, slingRequestProcessor, resourceResolver)
        .map(inputStream -> new Asset(InputStreamConverter.toByteBuffer(inputStream)))
        .orElseThrow();
  }

}
