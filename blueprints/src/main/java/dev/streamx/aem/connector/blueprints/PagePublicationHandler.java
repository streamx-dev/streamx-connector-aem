package dev.streamx.aem.connector.blueprints;

import dev.streamx.blueprints.data.Page;
import dev.streamx.sling.connector.IngestedData;
import dev.streamx.sling.connector.PublicationHandler;
import dev.streamx.sling.connector.PublishData;
import dev.streamx.sling.connector.UnpublishData;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;

import dev.streamx.sling.connector.util.DefaultSlingUriBuilder;
import org.apache.commons.io.IOUtils;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.uri.SlingUri;
import org.apache.sling.engine.SlingRequestProcessor;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.propertytypes.ServiceDescription;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(service = PublicationHandler.class)
@Designate(ocd = PagePublicationHandlerConfig.class)
@ServiceDescription("Publication handler for AEM Pages")
public class PagePublicationHandler implements PublicationHandler<Page> {

  private static final Logger LOG = LoggerFactory.getLogger(PagePublicationHandler.class);

  @Reference
  private SlingRequestProcessor requestProcessor;

  @Reference
  PageDataService pageDataService;

  @Reference
  private ResourceResolverFactory resolverFactory;

  private boolean enabled;
  private String publicationChannel;

  @Activate
  private void activate(PagePublicationHandlerConfig config) {
    enabled = config.enabled();
    publicationChannel = config.publication_channel();
  }

  @Override
  public String getId() {
    return this.getClass().getSimpleName();
  }

  @Override
  public boolean canHandle(IngestedData ingestedData) {
    SlingUri slingUri = ingestedData.uriToIngest();
    return enabled
        && pageDataService.isPage(slingUri)
        && !new XFCandidate(resolverFactory, slingUri).isXF();
  }

  @Override
  public PublishData<Page> getPublishData(String resourcePath) {
    try (ResourceResolver resourceResolver = createResourceResolver()) {
      Resource resource = resourceResolver.getResource(resourcePath);

      if (resource == null) {
        LOG.error("Resource not found when trying to publish it: {}", resourcePath);
        return null;
      }

      return new PublishData<>(
          getPagePath(resourcePath),
          publicationChannel,
          Page.class,
          getPageModel(resource));
    }
  }

  @Override
  public UnpublishData<Page> getUnpublishData(String resourcePath) {
    return new UnpublishData<>(
        getPagePath(resourcePath),
        publicationChannel,
        Page.class);
  }

  private static String getPagePath(String resourcePath) {
    return resourcePath + ".html";
  }

  private Page getPageModel(Resource resource) {
    try {
      InputStream inputStream = pageDataService.getStorageData(resource);
      return new Page(ByteBuffer.wrap(IOUtils.toByteArray(inputStream)));
    } catch (IOException e) {
      throw new UncheckedIOException("Cannot create page model", e);
    }
  }

  private ResourceResolver createResourceResolver() {
    try {
      return resolverFactory.getAdministrativeResourceResolver(null);
    } catch (LoginException e) {
      throw new IllegalStateException("Cannot create ResourceResolver", e);
    }
  }

}
