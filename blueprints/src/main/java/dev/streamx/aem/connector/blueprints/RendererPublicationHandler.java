package dev.streamx.aem.connector.blueprints;

import dev.streamx.blueprints.data.Renderer;
import dev.streamx.sling.connector.ResourceInfo;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.propertytypes.ServiceDescription;
import org.osgi.service.metatype.annotations.Designate;

@Component
@Designate(ocd = RendererPublicationHandlerConfig.class)
@ServiceDescription("Publication handler for renderers")
public class RendererPublicationHandler extends BasePublicationHandler<Renderer> {

  private final PageDataService pageDataService;
  private final AtomicReference<RendererPublicationHandlerConfig> config;

  @Activate
  public RendererPublicationHandler(
      @Reference PageDataService pageDataService,
      @Reference ResourceResolverFactory resolverFactory,
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

    if (!pageDataService.isPageTemplateByResourcePath(resource)) {
      return false;
    }

    try (ResourceResolver resourceResolver = createResourceResolver()) {
      return ResourcePrimaryNodeTypeChecker.isPage(resource, resourceResolver);
    }
  }

  @Override
  protected String getPublicationChannel() {
    return config.get().publication_channel();
  }

  @Override
  protected Renderer generateModel(Resource resource, ResourceResolver resourceResolver) {
    String content = pageDataService.readContentAsHtml(resource, resourceResolver);
    return new Renderer(content);
  }

  @Override
  protected String getPublicationKey(String resourcePath) {
    return resourcePath + ".html";
  }
}
