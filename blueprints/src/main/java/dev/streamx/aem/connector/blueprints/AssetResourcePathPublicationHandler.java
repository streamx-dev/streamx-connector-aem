package dev.streamx.aem.connector.blueprints;

import dev.streamx.blueprints.data.Asset;
import dev.streamx.sling.connector.PublicationHandler;
import dev.streamx.sling.connector.handlers.resourcepath.ResourcePathPublicationHandler;
import dev.streamx.sling.connector.handlers.resourcepath.ResourcePathPublicationHandlerConfig;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.engine.SlingRequestProcessor;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(
    service = {
        ResourcePathPublicationHandler.class, PublicationHandler.class, AssetResourcePathPublicationHandler.class
    },
    immediate = true,
    configurationPolicy = ConfigurationPolicy.REQUIRE
)
@Designate(ocd = AssetResourcePathPublicationHandlerConfig.class)
public class AssetResourcePathPublicationHandler extends ResourcePathPublicationHandler<Asset> {

  private static final Logger LOG = LoggerFactory.getLogger(AssetResourcePathPublicationHandler.class);
  private final AtomicReference<AssetResourcePathPublicationHandlerConfig> config;

  @Activate
  public AssetResourcePathPublicationHandler(
      @Reference(cardinality = ReferenceCardinality.MANDATORY)
      ResourceResolverFactory resourceResolverFactory,
      @Reference(cardinality = ReferenceCardinality.MANDATORY)
      SlingRequestProcessor slingRequestProcessor,
      AssetResourcePathPublicationHandlerConfig config
  ) {
    super(resourceResolverFactory, slingRequestProcessor);
    this.config = new AtomicReference<>(config);
  }

  @Modified
  void configure(AssetResourcePathPublicationHandlerConfig config) {
    this.config.set(config);
  }

  @Override
  public ResourcePathPublicationHandlerConfig configuration() {
    return new ResourcePathPublicationHandlerConfig() {
      @Override
      public String resourcePathRegex() {
        return config.get().assets_path_regexp();
      }

      @Override
      public String channel() {
        return config.get().publication_channel();
      }

      @Override
      public boolean isEnabled() {
        return config.get().enabled();
      }
    };
  }

  @Override
  public Class<Asset> modelClass() {
    return Asset.class;
  }

  @Override
  public Asset model(InputStream inputStream) {
    return new Asset(ByteBuffer.wrap(toByteArray(inputStream)));
  }

  private byte[] toByteArray(InputStream inputStream) {
    try {
      return IOUtils.toByteArray(inputStream);
    } catch (IOException exception) {
      LOG.error("Cannot convert input stream to byte array", exception);
      return new byte[NumberUtils.INTEGER_ZERO];
    }
  }

  @Override
  public String getId() {
    return this.getClass().getSimpleName();
  }
}
