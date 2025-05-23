package dev.streamx.aem.connector.blueprints;

import dev.streamx.blueprints.data.Renderer;
import dev.streamx.sling.connector.PublishData;
import dev.streamx.sling.connector.ResourceInfo;
import dev.streamx.sling.connector.UnpublishData;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.commons.io.IOUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.propertytypes.ServiceDescription;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
@Designate(ocd = RendererPublicationHandlerConfig.class)
@ServiceDescription("Publication handler for renderers")
public class RendererPublicationHandler extends BasePublicationHandler<Renderer> {

  private static final Logger LOG = LoggerFactory.getLogger(RendererPublicationHandler.class);

  private final PageDataService pageDataService;
  private final AtomicReference<RendererPublicationHandlerConfig> config;

  @Activate
  public RendererPublicationHandler(
      @Reference(cardinality = ReferenceCardinality.MANDATORY)
      PageDataService pageDataService,
      @Reference(cardinality = ReferenceCardinality.MANDATORY)
      ResourceResolverFactory resolverFactory,
      RendererPublicationHandlerConfig config
  ) {
    super(resolverFactory);
    this.pageDataService = pageDataService;
    this.config = new AtomicReference<>(config);
  }

  @Modified
  void configure(RendererPublicationHandlerConfig config) {
    this.config.set(config);
  }

  @Override
  public String getId() {
    return this.getClass().getSimpleName();
  }

  @Override
  public boolean canHandle(ResourceInfo resource) {
    try (ResourceResolver resourceResolver = createResourceResolver()) {
      return config.get().enabled()
          && pageDataService.isPageTemplate(resource, resourceResolver);
    }
  }

  @Override
  public PublishData<Renderer> getPublishData(String resourcePath) {
    try (ResourceResolver resourceResolver = createResourceResolver()) {
      Resource resource = resourceResolver.getResource(resourcePath);

      if (resource == null) {
        LOG.error("Resource not found when trying to publish it: {}", resourcePath);
        return null;
      }

      return new PublishData<>(
          getStoragePath(resourcePath),
          config.get().publication_channel(),
          Renderer.class,
          resolveData(resource, resourceResolver));
    }
  }

  @Override
  public UnpublishData<Renderer> getUnpublishData(String resourcePath) {
    return new UnpublishData<>(
        getStoragePath(resourcePath),
        config.get().publication_channel(),
        Renderer.class);
  }

  private Renderer resolveData(Resource resource, ResourceResolver resourceResolver) {
    try (InputStream inputStream = getStorageData(resource, resourceResolver)) {
      return new Renderer(ByteBuffer.wrap(IOUtils.toByteArray(inputStream)));
    } catch (IOException e) {
      throw new UncheckedIOException("Cannot create renderer model", e);
    }
  }

  private String getStoragePath(String resourcePath) {
    return resourcePath + ".html";
  }


  public InputStream getStorageData(Resource resource,
      ResourceResolver resourceResolver) throws IOException {
    return pageDataService.getStorageData(resource, resourceResolver);
  }
}
