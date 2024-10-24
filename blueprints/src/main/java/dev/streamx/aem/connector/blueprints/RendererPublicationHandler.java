package dev.streamx.aem.connector.blueprints;

import dev.streamx.blueprints.data.Renderer;
import dev.streamx.sling.connector.PublicationHandler;
import dev.streamx.sling.connector.PublishData;
import dev.streamx.sling.connector.UnpublishData;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import org.apache.commons.io.IOUtils;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
@Designate(ocd = RendererPublicationHandlerConfig.class)
public class RendererPublicationHandler implements PublicationHandler<Renderer> {

  private static final Logger LOG = LoggerFactory.getLogger(RendererPublicationHandler.class);

  @Reference
  private PageDataService pageDataService;

  @Reference
  private ResourceResolverFactory resolverFactory;

  private String publicationChannel;
  private boolean enabled;

  @Activate
  @Modified
  private void activate(RendererPublicationHandlerConfig config) {
    publicationChannel = config.publication_channel();
    enabled = config.enabled();
  }

  @Override
  public String getId() {
    return "streamx-renderer";
  }

  @Override
  public boolean canHandle(String resourcePath) {
    return enabled && pageDataService.isPageTemplate(resourcePath);
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
          publicationChannel,
          Renderer.class,
          resolveData(resource));
    }
  }

  @Override
  public UnpublishData<Renderer> getUnpublishData(String resourcePath) {
    return new UnpublishData<>(
        getStoragePath(resourcePath),
        publicationChannel,
        Renderer.class);
  }

  private Renderer resolveData(Resource resource) {
    try (InputStream inputStream = getStorageData(resource)) {
      return new Renderer(ByteBuffer.wrap(IOUtils.toByteArray(inputStream)));
    } catch (IOException e) {
      throw new UncheckedIOException("Cannot create renderer model", e);
    }
  }

  private String getStoragePath(String resourcePath) {
    return resourcePath + ".html";
  }


  public InputStream getStorageData(Resource resource) throws IOException {
    return pageDataService.getStorageData(resource);
  }

  private ResourceResolver createResourceResolver() {
    try {
      return resolverFactory.getAdministrativeResourceResolver(null);
    } catch (LoginException e) {
      throw new IllegalStateException("Cannot create ResourceResolver", e);
    }
  }
}
