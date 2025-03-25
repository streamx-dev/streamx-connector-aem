package dev.streamx.aem.connector.blueprints;

import dev.streamx.blueprints.data.RenderingContext;
import dev.streamx.blueprints.data.RenderingContext.OutputType;
import dev.streamx.sling.connector.PublicationHandler;
import dev.streamx.sling.connector.PublishData;
import dev.streamx.sling.connector.UnpublishData;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.LoginException;
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
public class RenderingContextPublicationHandler implements PublicationHandler<RenderingContext> {

  private static final Logger LOG =
      LoggerFactory.getLogger(RenderingContextPublicationHandler.class);

  @Reference
  private ResourceResolverFactory resolverFactory;

  @Reference
  private PageDataService pageDataService;

  private String publicationChannel;
  private boolean enabled;

  @Activate
  @Modified
  private void activate(RenderingContextPublicationHandlerConfig config) {
    publicationChannel = config.publication_channel();
    enabled = config.enabled();
  }

  @Override
  public String getId() {
    return this.getClass().getSimpleName();
  }

  @Override
  public boolean canHandle(String resourcePath) {
    return enabled && pageDataService.isPageTemplate(resourcePath);
  }

  @Override
  public PublishData<RenderingContext> getPublishData(String resourcePath) {
    try (ResourceResolver resourceResolver = createResourceResolver()) {
      Resource resource = resourceResolver.getResource(resourcePath);
      if (resource == null) {
        LOG.error("Resource not found when trying to publish it: {}", resourcePath);
        return null;
      }
      RenderingContext renderingContext = resolveData(resource);
      if (renderingContext != null) {
        return new PublishData<>(
            getKeyForTemplateResource(resource),
            publicationChannel,
            RenderingContext.class,
            renderingContext);
      }
      return null;
    }
  }

  @Override
  public UnpublishData<RenderingContext> getUnpublishData(String resourcePath) {
    return new UnpublishData<>(
        resourcePath,
        publicationChannel,
        RenderingContext.class);
  }

  private RenderingContext resolveData(Resource resource) {
    ValueMap properties = Optional.ofNullable(resource.getChild("jcr:content"))
        .map(Resource::getValueMap)
        .orElse(ValueMap.EMPTY);
    String dataKeyMatchPattern = properties.get("dataKeyMatchPattern", String.class);
    String outputKeyTemplate = properties.get("outputKeyTemplate", String.class);
    if (StringUtils.isNoneBlank(dataKeyMatchPattern, outputKeyTemplate)) {
      // Use same key as is used for the Renderer published for the template page.
      String rendererKey = getKeyForTemplateResource(resource);
      return new RenderingContext(rendererKey, dataKeyMatchPattern, outputKeyTemplate,
          OutputType.PAGE);
    }
    LOG.info("Cannot prepare publish data for {}. Resource doesn't contain required properties.",
        resource.getPath());
    return null;
  }

  private String getKeyForTemplateResource(Resource resource) {
    return resource.getPath() + ".html";
  }

  private ResourceResolver createResourceResolver() {
    try {
      return resolverFactory.getAdministrativeResourceResolver(null);
    } catch (LoginException e) {
      throw new IllegalStateException("Cannot create ResourceResolver", e);
    }
  }

}
