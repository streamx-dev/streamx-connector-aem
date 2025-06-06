package dev.streamx.aem.connector.blueprints;

import dev.streamx.blueprints.data.Fragment;
import dev.streamx.sling.connector.PublicationHandler;
import dev.streamx.sling.connector.PublishData;
import dev.streamx.sling.connector.ResourceInfo;
import dev.streamx.sling.connector.UnpublishData;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.ResourceUtil;
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
  public PublishData<Fragment> getPublishData(String resourcePath) {
    try (ResourceResolver resourceResolver = createResourceResolver()) {
      return Optional.of(resourceResolver.resolve(resourcePath))
          .filter(resolvedResource -> !ResourceUtil.isNonExistingResource(resolvedResource))
          .map(resource -> toPublishData(resource, resourceResolver))
          .orElseThrow(() -> new IllegalArgumentException("Resource not found: " + resourcePath));
    }
  }

  @Override
  public UnpublishData<Fragment> getUnpublishData(String resourcePath) {
    return new UnpublishData<>(
        toStreamXKey(resourcePath),
        config.get().publication_channel(),
        Fragment.class
    );
  }

  private static String toStreamXKey(String resourcePath) {
    return String.format("%s.html", resourcePath);
  }

  private PublishData<Fragment> toPublishData(Resource resource, ResourceResolver resourceResolver) {
    Resource childResource = resource.getChild(config.get().rel_path_to_node_with_jcr_prop_for_sx_type());
    Map<String, String> messageProps = getSxTypeAsMap(childResource, config.get().jcr_prop_name_for_sx_type());

    return new PublishData<>(
        toStreamXKey(resource.getPath()),
        config.get().publication_channel(),
        Fragment.class,
        toFragment(resource, resourceResolver),
        messageProps
    );
  }

  private Fragment toFragment(Resource resource, ResourceResolver resourceResolver) {
    String content = pageDataService.getStorageData(resource, resourceResolver);
    return new Fragment(content);
  }
}
