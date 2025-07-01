package dev.streamx.aem.connector.blueprints;

import dev.streamx.sling.connector.PublicationHandler;
import dev.streamx.sling.connector.PublishData;
import dev.streamx.sling.connector.ResourceInfo;
import dev.streamx.sling.connector.UnpublishData;
import java.lang.reflect.ParameterizedType;
import java.util.Collections;
import java.util.Map;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract class BasePublicationHandler<T> implements PublicationHandler<T> {

  static final String SX_TYPE = "sx:type";
  private static final Logger LOG = LoggerFactory.getLogger(BasePublicationHandler.class);

  private final ResourceResolverFactory resolverFactory;
  private final Class<T> dataClass;

  BasePublicationHandler(ResourceResolverFactory resolverFactory) {
    this.resolverFactory = resolverFactory;
    dataClass = (Class<T>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
  }

  @Override
  public final String getId() {
    return this.getClass().getSimpleName();
  }

  @Override
  public final PublishData<T> getPublishData(ResourceInfo resourceInfo) {
    String resourcePath = resourceInfo.getPath();
    try (ResourceResolver resourceResolver = createResourceResolver()) {
      Resource resource = resourceResolver.getResource(resourcePath);
      if (resource == null) {
        LOG.error("Resource not found when trying to publish it: {}", resourcePath);
        return null;
      }

      T model = generateModel(resource, resourceResolver);
      if (model == null) {
        return null;
      }

      return new PublishData<>(
          getPublicationKey(resourcePath),
          getPublicationChannel(),
          dataClass,
          model,
          getMessageProps(resourceInfo)
      );
    }
  }

  @Override
  public final UnpublishData<T> getUnpublishData(ResourceInfo resourceInfo) {
    return new UnpublishData<>(
        getPublicationKey(resourceInfo.getPath()),
        getPublicationChannel(),
        dataClass,
        getMessageProps(resourceInfo)
    );
  }

  protected String getPublicationKey(String resourcePath) {
    return resourcePath;
  }

  protected Map<String, String> getMessageProps(ResourceInfo resourceInfo) {
    return Collections.emptyMap();
  }

  protected abstract String getPublicationChannel();

  protected abstract T generateModel(Resource resource, ResourceResolver resourceResolver);

  protected ResourceResolver createResourceResolver() {
    try {
      return resolverFactory.getAdministrativeResourceResolver(null);
    } catch (LoginException e) {
      throw new IllegalStateException("Cannot create ResourceResolver", e);
    }
  }

  protected static Map<String, String> getSxTypeAsMap(ResourceInfo resourceInfo, String childRelativePath, String jcrPropertyForSxType) {
    return getSxTypeAsMap(resourceInfo, childRelativePath + "/" + jcrPropertyForSxType);
  }

  protected static Map<String, String> getSxTypeAsMap(ResourceInfo resourceInfo, String jcrPropertyForSxType) {
    String sxType = resourceInfo.getProperty(jcrPropertyForSxType);
    if (sxType == null) {
      return Collections.emptyMap();
    }
    return Map.of(SX_TYPE, sxType);
  }
}
