package dev.streamx.aem.connector.blueprints;

import com.drew.lang.annotations.Nullable;
import dev.streamx.sling.connector.PublicationHandler;
import dev.streamx.sling.connector.PublishData;
import dev.streamx.sling.connector.UnpublishData;
import java.lang.reflect.ParameterizedType;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.ValueMap;
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
  public final PublishData<T> getPublishData(String resourcePath) {
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
          getMessageProps(resource)
      );
    }
  }

  @Override
  public final UnpublishData<T> getUnpublishData(String resourcePath) {
    return new UnpublishData<>(
        getPublicationKey(resourcePath),
        getPublicationChannel(),
        dataClass
    );
  }

  protected String getPublicationKey(String resourcePath) {
    return resourcePath;
  }

  protected Map<String, String> getMessageProps(Resource resource) {
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

  protected static Map<String, String> getSxTypeAsMap(Resource resource, String childRelativePath, String jcrPropertyForSxType) {
    return getSxTypeAsMap(resource, childRelativePath + "/" + jcrPropertyForSxType);
  }

  protected static Map<String, String> getSxTypeAsMap(@Nullable Resource resource, String jcrPropertyForSxType) {
    return Optional.ofNullable(resource)
        .map(res -> res.adaptTo(ValueMap.class))
        .map(valueMap -> valueMap.get(jcrPropertyForSxType, String.class))
        .map(sxType -> Map.of(SX_TYPE, sxType))
        .orElseGet(Collections::emptyMap);
  }
}
