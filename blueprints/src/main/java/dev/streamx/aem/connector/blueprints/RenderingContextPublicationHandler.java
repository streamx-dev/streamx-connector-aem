package dev.streamx.aem.connector.blueprints;

import dev.streamx.blueprints.data.RenderingContext;
import dev.streamx.blueprints.data.RenderingContext.OutputFormat;
import dev.streamx.sling.connector.ResourceInfo;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.ValueMap;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.propertytypes.ServiceDescription;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
@Designate(ocd = RenderingContextPublicationHandlerConfig.class)
@ServiceDescription("Publication handler for rendering contexts")
public class RenderingContextPublicationHandler extends BasePublicationHandler<RenderingContext> {

  private static final Logger LOG =
      LoggerFactory.getLogger(RenderingContextPublicationHandler.class);

  private final PageDataService pageDataService;
  private final AtomicReference<RenderingContextPublicationHandlerConfig> config;

  @Activate
  public RenderingContextPublicationHandler(
      @Reference PageDataService pageDataService,
      @Reference ResourceResolverFactory resolverFactory,
      RenderingContextPublicationHandlerConfig config
  ) {
    super(resolverFactory);
    this.pageDataService = pageDataService;
    this.config = new AtomicReference<>(config);
  }

  @Modified
  void configure(RenderingContextPublicationHandlerConfig config) {
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
  protected RenderingContext generateModel(Resource resource, ResourceResolver resourceResolver) {
    ValueMap properties = Optional.ofNullable(resource.getChild("jcr:content"))
        .map(Resource::getValueMap)
        .orElse(ValueMap.EMPTY);
    String dataKeyMatchPattern = properties.get("dataKeyMatchPattern", String.class);
    String dataTypeMatchPattern = properties.get("dataTypeMatchPattern", String.class);
    String outputKeyTemplate = properties.get("outputKeyTemplate", String.class);
    String outputTypeTemplate = properties.get("outputTypeTemplate", String.class);
    if (StringUtils.isNoneBlank(dataKeyMatchPattern, outputKeyTemplate)) {
      String rendererKey = resource.getPath();
      return new RenderingContext(
          rendererKey,
          dataKeyMatchPattern,
          dataTypeMatchPattern,
          outputKeyTemplate,
          outputTypeTemplate,
          OutputFormat.PAGE
      );
    }
    LOG.info("Cannot prepare publish data for {}. Resource doesn't contain required properties.",
        resource.getPath());
    return null;
  }
}
