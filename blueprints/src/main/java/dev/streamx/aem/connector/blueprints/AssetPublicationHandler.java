package dev.streamx.aem.connector.blueprints;

import com.day.cq.dam.api.Rendition;
import dev.streamx.blueprints.data.Asset;
import dev.streamx.sling.connector.PublicationHandler;
import dev.streamx.sling.connector.ResourceInfo;
import dev.streamx.sling.connector.util.SimpleInternalRequest;
import java.io.InputStream;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.uri.SlingUri;
import org.apache.sling.api.uri.SlingUriBuilder;
import org.apache.sling.engine.SlingRequestProcessor;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
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
      @Reference ResourceResolverFactory resolverFactory,
      @Reference SlingRequestProcessor slingRequestProcessor,
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
  protected String getPublicationChannel() {
    return config.get().publication_channel();
  }

  @Override
  protected Asset generateModel(Resource resource, ResourceResolver resourceResolver) {
    if (isContentFragment(resource)) {
      LOG.info("Resource is a Content Fragment, skipping publication: {}", resource.getPath());
      return null;
    }

    return generateAssetModel(resource, resourceResolver);
  }

  @Override
  protected Map<String, String> getMessageProps(Resource resource) {
    return getSxTypeAsMap(resource, config.get().jcr_prop_name_for_sx_type());
  }

  private static boolean isContentFragment(Resource existingAssetResource) {
    return Optional.ofNullable(existingAssetResource.getChild("jcr:content"))
        .map(Resource::getValueMap)
        .map(properties -> Boolean.TRUE.equals(properties.get("contentFragment")))
        .orElse(false);
  }

  private Asset generateAssetModel(Resource resource, ResourceResolver resourceResolver) {
    LOG.trace("Generating {} out of {}", Asset.class, resource.getPath());
    SlingUri slingUri = SlingUriBuilder.parse(resource.getPath(), resourceResolver).build();

    InputStream assetContentStream = Optional
        .ofNullable(resource.adaptTo(com.day.cq.dam.api.Asset.class))
        .map(com.day.cq.dam.api.Asset::getOriginal)
        .map(Rendition::getStream)
        .or(() -> new SimpleInternalRequest(slingUri, slingRequestProcessor, resourceResolver).getResponseAsInputStream())
        .orElseThrow();

    return new Asset(InputStreamConverter.toByteBuffer(assetContentStream));
  }

}
