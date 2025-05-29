package dev.streamx.aem.connector.blueprints;

import dev.streamx.blueprints.data.Renderer;
import dev.streamx.sling.connector.PublishData;
import dev.streamx.sling.connector.ResourceInfo;
import dev.streamx.sling.connector.UnpublishData;
import java.util.concurrent.atomic.AtomicReference;
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
  public boolean canHandle(ResourceInfo resource) {
    if (!config.get().enabled()) {
      return false;
    }

    try (ResourceResolver resourceResolver = createResourceResolver()) {
      return pageDataService.isPageTemplate(resource, resourceResolver);
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
    String content = pageDataService.getStorageData(resource, resourceResolver);
    return new Renderer(content);
  }

  private String getStoragePath(String resourcePath) {
    return resourcePath + ".html";
  }
}
