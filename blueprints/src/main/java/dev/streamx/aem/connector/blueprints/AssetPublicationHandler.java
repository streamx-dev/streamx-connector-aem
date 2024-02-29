package dev.streamx.aem.connector.blueprints;

import com.day.cq.dam.api.Asset;
import com.day.cq.dam.api.Rendition;
import dev.streamx.sling.connector.PublicationHandler;
import dev.streamx.sling.connector.PublishData;
import dev.streamx.sling.connector.UnpublishData;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import org.apache.commons.io.IOUtils;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(service = PublicationHandler.class)
@Designate(ocd = AssetPublicationHandlerConfig.class)
public class AssetPublicationHandler implements PublicationHandler<Data> {

  private static final Logger LOG = LoggerFactory.getLogger(AssetPublicationHandler.class);

  private static final String ID = "streamx-asset";
  private static final String CHANNEL = "assets";

  @Reference
  private ResourceResolverFactory resolverFactory;

  private boolean enabled;
  private String assetsPathRegexp;

  private static String getAssetPath(String resourcePath) {
    // AEM path is /content/dam/<project>
    // In StreamX we want /published/<project>/assets
    Path path = Paths.get(resourcePath);
    if (path.getNameCount() <= 3) {
      LOG.warn("Cannot get project name from path: {}", resourcePath);
      return null;
    }

    String projectName = path.getName(2).toString();
    Path assetPathRelativeToProject = path.subpath(3, path.getNameCount());
    String renditionName = getRenditionName(path.getFileName().toString());
    if (renditionName == null) {
      LOG.warn("Cannot get rendition name from path: {}", resourcePath);
      return null;
    }

    // Also, we will add /jcr:content/renditions/original.<extension>
    return "/published/" + projectName + "/assets/" + assetPathRelativeToProject
        + "/jcr:content/renditions/" + renditionName;
  }

  private static String getRenditionName(String fileName) {
    int dotIndex = fileName.indexOf(".");
    if (dotIndex < 0) {
      return null;
    }
    return "original." + fileName.substring(dotIndex + 1);
  }

  @Activate
  private void activate(AssetPublicationHandlerConfig config) {
    enabled = config.enabled();
    assetsPathRegexp = config.assets_path_regexp();
  }

  @Override
  public String getId() {
    return ID;
  }

  @Override
  public boolean canHandle(String resourcePath) {
    return enabled
        && resourcePath.matches(assetsPathRegexp)
        && !resourcePath.contains("jcr:content")
        && getAssetPath(resourcePath) != null;
  }

  @Override
  public PublishData<Data> getPublishData(String resourcePath) {
    try (ResourceResolver resourceResolver = createResourceResolver()) {
      Resource resource = resourceResolver.getResource(resourcePath);

      if (resource == null) {
        LOG.error("Resource not found when trying to publish it: {}", resourcePath);
        return null;
      }

      return new PublishData<>(
          getAssetPath(resource.getPath()),
          CHANNEL,
          Data.class,
          getAssetModel(resource));
    }
  }

  private ResourceResolver createResourceResolver() {
    try {
      return resolverFactory.getAdministrativeResourceResolver(null);
    } catch (LoginException e) {
      throw new IllegalStateException("Cannot create ResourceResolver", e);
    }
  }

  @Override
  public UnpublishData<Data> getUnpublishData(String resourcePath) {
    return new UnpublishData<>(
        getAssetPath(resourcePath),
        CHANNEL,
        Data.class);
  }

  private Data getAssetModel(Resource resource) {
    InputStream inputStream = Optional.of(resource)
        .map(assetResource -> assetResource.adaptTo(Asset.class))
        .map(Asset::getOriginal)
        .map(Rendition::getStream)
        .orElse(null);
    if (inputStream == null) {
      throw new IllegalStateException(
          "Cannot get InputStream from asset's original rendition: " + resource.getPath());
    }
    try {
      return new Data(ByteBuffer.wrap(IOUtils.toByteArray(inputStream)));
    } catch (IOException e) {
      throw new UncheckedIOException("Cannot create asset model", e);
    }
  }

}
