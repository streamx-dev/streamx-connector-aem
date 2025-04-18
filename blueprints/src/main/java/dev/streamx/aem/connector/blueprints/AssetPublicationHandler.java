package dev.streamx.aem.connector.blueprints;

import dev.streamx.blueprints.data.Asset;
import dev.streamx.sling.connector.PublicationHandler;
import dev.streamx.sling.connector.PublishData;
import dev.streamx.sling.connector.UnpublishData;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.resource.ValueMap;
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
public class AssetPublicationHandler implements PublicationHandler<Asset> {

  private static final Logger LOG = LoggerFactory.getLogger(AssetPublicationHandler.class);

  private final ResourceResolverFactory resolverFactory;
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
    this.resolverFactory = resolverFactory;
    this.slingRequestProcessor = slingRequestProcessor;
    this.config = new AtomicReference<>(config);
  }

  @Modified
  void configure(AssetPublicationHandlerConfig config) {
    this.config.set(config);
  }

  @Override
  public String getId() {
    return this.getClass().getSimpleName();
  }

  @Override
  public boolean canHandle(String resourcePath) {
    try (ResourceResolver resourceResolver = createResourceResolver()) {
      SlingUri slingUri = SlingUriBuilder.parse(resourcePath, resourceResolver).build();
      AssetCandidate assetCandidate = new AssetCandidate(resolverFactory, slingUri);
      boolean canHandle = config.get().enabled()
          && resourcePath.matches(config.get().assets_path_regexp())
          && assetCandidate.isAsset();
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
      Map<String, String> messageProps = Optional.ofNullable(resource.adaptTo(ValueMap.class))
          .map(
              valueMap -> valueMap.get(
                  config.get().jcr$_$prop$_$name_for$_$sx$_$type(), String.class
              )
          ).map(propertyValue -> Map.of(SXType.VALUE, propertyValue))
          .orElse(Map.of());
      if (ResourceUtil.isNonExistingResource(resource)) {
        LOG.error("Resource not found for publish data generation: {}", slingUri);
        return null;
      }
      return generatePublishData(slingUri, messageProps);
    }
  }

  private PublishData<Asset> generatePublishData(
      SlingUri slingUri,
      Map<String, String> messageProps
  ) {
    LOG.trace("Generating publish data for '{}'", slingUri);
    Asset asset = generateAssetModel(slingUri);
    return new PublishData<>(
        slingUri.toString(), config.get().publication_channel(), Asset.class,
        asset, messageProps
    );
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
        resourcePath, config.get().publication_channel(), Asset.class
    );
  }

  private byte[] toByteArray(InputStream inputStream) {
    try {
      return IOUtils.toByteArray(inputStream);
    } catch (IOException exception) {
      LOG.error("Cannot convert input stream to byte array", exception);
      return new byte[NumberUtils.INTEGER_ZERO];
    }
  }

  private Asset generateAssetModel(SlingUri slingUri) {
    LOG.trace("Generating {} out of {}", Asset.class, slingUri);
    AssetContent assetContent = new AssetContent(slingUri, slingRequestProcessor, resolverFactory);
    return assetContent.get()
        .map(inputStream -> new Asset(ByteBuffer.wrap(toByteArray(inputStream))))
        .orElseThrow();
  }

}
