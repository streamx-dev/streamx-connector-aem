package dev.streamx.aem.connector.blueprints;

import dev.streamx.blueprints.data.WebResource;
import dev.streamx.sling.connector.PublicationHandler;
import dev.streamx.sling.connector.handlers.resourcepath.ResourcePathPublicationHandler;
import dev.streamx.sling.connector.handlers.resourcepath.ResourcePathPublicationHandlerConfig;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.engine.SlingRequestProcessor;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.metatype.annotations.Designate;

@Component(
    service = {
        ResourcePathPublicationHandler.class, PublicationHandler.class, ClientlibsPublicationHandler.class
    },
    immediate = true,
    configurationPolicy = ConfigurationPolicy.REQUIRE
)
@Designate(ocd = ClientlibsPublicationHandlerConfig.class)
public class ClientlibsPublicationHandler extends ResourcePathPublicationHandler<WebResource> {

  private final AtomicReference<ClientlibsPublicationHandlerConfig> config;

  @Activate
  public ClientlibsPublicationHandler(
      @Reference(cardinality = ReferenceCardinality.MANDATORY)
      ResourceResolverFactory resourceResolverFactory,
      @Reference(cardinality = ReferenceCardinality.MANDATORY)
      SlingRequestProcessor slingRequestProcessor,
      ClientlibsPublicationHandlerConfig config
  ) {
    super(resourceResolverFactory, slingRequestProcessor);
    this.config = new AtomicReference<>(config);
  }

  @Modified
  void configure(ClientlibsPublicationHandlerConfig config) {
    this.config.set(config);
  }

  @Override
  public ResourcePathPublicationHandlerConfig configuration() {
    return new ResourcePathPublicationHandlerConfig() {
      @Override
      public String resourcePathRegex() {
        return config.get().clientlibs_path_regexp();
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
  public Class<WebResource> modelClass() {
    return WebResource.class;
  }

  @Override
  public WebResource model(InputStream inputStream) {
    return new WebResource(InputStreamConverter.toByteBuffer(inputStream));
  }

  @Override
  public String getId() {
    return this.getClass().getSimpleName();
  }
}
