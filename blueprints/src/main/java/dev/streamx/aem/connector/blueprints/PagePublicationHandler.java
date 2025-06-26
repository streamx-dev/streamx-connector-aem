package dev.streamx.aem.connector.blueprints;

import dev.streamx.blueprints.data.Page;
import dev.streamx.sling.connector.PublicationHandler;
import dev.streamx.sling.connector.PublishData;
import dev.streamx.sling.connector.ResourceInfo;
import dev.streamx.sling.connector.UnpublishData;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(service = PublicationHandler.class)
@Designate(ocd = PagePublicationHandlerConfig.class)
@ServiceDescription("Publication handler for AEM Pages")
public class PagePublicationHandler extends BasePublicationHandler<Page> {

  private static final Logger LOG = LoggerFactory.getLogger(PagePublicationHandler.class);

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
  public PublishData<Page> getPublishData(String resourcePath) {
    try (ResourceResolver resourceResolver = createResourceResolver()) {
      Resource resource = resourceResolver.getResource(resourcePath);

      if (resource == null) {
        LOG.error("Resource not found when trying to publish it: {}", resourcePath);
        return null;
      }

      Map<String, String> messageProps = getSxTypeAsMap(
          resource,
          config.get().rel_path_to_node_with_jcr_prop_for_sx_type(),
          config.get().jcr_prop_name_for_sx_type()
      );

      return new PublishData<>(
          getPagePath(resourcePath),
          config.get().publication_channel(),
          Page.class,
          getPageModel(resource, resourceResolver),
          messageProps
      );
    }
  }

  @Override
  public UnpublishData<Page> getUnpublishData(String resourcePath) {
    return new UnpublishData<>(
        getPagePath(resourcePath),
        config.get().publication_channel(),
        Page.class);
  }

  private static String getPagePath(String resourcePath) {
    return resourcePath + ".html";
  }

  private Page getPageModel(Resource resource, ResourceResolver resourceResolver) {
    String content = pageDataService.getStorageData(resource, resourceResolver);
    return new Page(content);
  }

}
