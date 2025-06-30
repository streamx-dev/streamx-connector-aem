package dev.streamx.aem.connector.blueprints;

import dev.streamx.blueprints.data.Page;
import dev.streamx.sling.connector.PublicationHandler;
import dev.streamx.sling.connector.ResourceInfo;
import java.util.Map;
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

@Component(service = PublicationHandler.class)
@Designate(ocd = PagePublicationHandlerConfig.class)
@ServiceDescription("Publication handler for AEM Pages")
public class PagePublicationHandler extends BasePublicationHandler<Page> {

  private final PageDataService pageDataService;
  private final AtomicReference<PagePublicationHandlerConfig> config;

  @Activate
  public PagePublicationHandler(
      @Reference PageDataService pageDataService,
      @Reference ResourceResolverFactory resolverFactory,
      PagePublicationHandlerConfig config
  ) {
    super(resolverFactory);
    this.pageDataService = pageDataService;
    this.config = new AtomicReference<>(config);
  }

  @Modified
  void configure(PagePublicationHandlerConfig config) {
    this.config.set(config);
  }

  @Override
  public boolean canHandle(ResourceInfo resource) {
    if (!config.get().enabled()) {
      return false;
    }

    if (ResourcePrimaryNodeTypeChecker.isXFPath(resource.getPath())) {
      return false;
    }

    if (!pageDataService.isPageByResourcePath(resource)) {
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
  protected Page generateModel(Resource resource, ResourceResolver resourceResolver) {
    String content = pageDataService.readContentAsHtml(resource, resourceResolver);
    return new Page(content);
  }

  @Override
  protected Map<String, String> getMessageProps(Resource resource) {
    return getSxTypeAsMap(
        resource,
        config.get().rel_path_to_node_with_jcr_prop_for_sx_type(),
        config.get().jcr_prop_name_for_sx_type()
    );
  }

  @Override
  protected String getPublicationKey(String resourcePath) {
    return resourcePath + ".html";
  }

}
