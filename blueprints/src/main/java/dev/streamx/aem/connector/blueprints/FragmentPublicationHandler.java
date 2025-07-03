package dev.streamx.aem.connector.blueprints;

import dev.streamx.blueprints.data.Fragment;
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
@Designate(ocd = FragmentPublicationHandlerConfig.class)
@ServiceDescription("Publication handler for Experience Fragments")
public class FragmentPublicationHandler extends BasePublicationHandler<Fragment> {

  private final PageDataService pageDataService;
  private final AtomicReference<FragmentPublicationHandlerConfig> config;

  @Activate
  public FragmentPublicationHandler(
      @Reference PageDataService pageDataService,
      @Reference ResourceResolverFactory resourceResolverFactory,
      FragmentPublicationHandlerConfig config
  ) {
    super(resourceResolverFactory);
    this.pageDataService = pageDataService;
    this.config = new AtomicReference<>(config);
  }

  @Modified
  void configure(FragmentPublicationHandlerConfig config) {
    this.config.set(config);
  }

  @Override
  public boolean canHandle(ResourceInfo resource) {
    if (!config.get().enabled()) {
      return false;
    }

    try (ResourceResolver resourceResolver = createResourceResolver()) {
      return ResourcePrimaryNodeTypeChecker.isXF(resource, resourceResolver);
    }
  }

  @Override
  protected String getPublicationChannel() {
    return config.get().publication_channel();
  }

  @Override
  protected String getPublicationKey(String resourcePath) {
    return String.format("%s.html", resourcePath);
  }

  @Override
  protected Fragment generateModel(Resource resource, ResourceResolver resourceResolver) {
    String content = pageDataService.readContentAsHtml(resource, resourceResolver);
    return new Fragment(content);
  }

  @Override
  protected Map<String, String> getMessageProperties(ResourceInfo resourceInfo) {
    return getSxTypeAsMap(
        resourceInfo,
        config.get().rel_path_to_node_with_jcr_prop_for_sx_type(),
        config.get().jcr_prop_name_for_sx_type()
    );
  }

}
