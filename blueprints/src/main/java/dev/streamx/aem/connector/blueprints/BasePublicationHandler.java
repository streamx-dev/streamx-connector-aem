package dev.streamx.aem.connector.blueprints;

import com.drew.lang.annotations.Nullable;
import dev.streamx.sling.connector.PublicationHandler;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.ValueMap;

abstract class BasePublicationHandler<T> implements PublicationHandler<T> {

  static final String SX_TYPE = "sx:type";

  protected final ResourceResolverFactory resolverFactory;

  BasePublicationHandler(ResourceResolverFactory resolverFactory) {
    this.resolverFactory = resolverFactory;
  }

  @Override
  public final String getId() {
    return this.getClass().getSimpleName();
  }

  protected ResourceResolver createResourceResolver() {
    try {
      return resolverFactory.getAdministrativeResourceResolver(null);
    } catch (LoginException e) {
      throw new IllegalStateException("Cannot create ResourceResolver", e);
    }
  }

  protected static Map<String, String> getSxTypeAsMap(Resource resource, String childRelativePath, String jcrPropNameForSxType) {
    Resource childResource = resource.getChild(childRelativePath);
    return getSxTypeAsMap(childResource, jcrPropNameForSxType);
  }

  protected static Map<String, String> getSxTypeAsMap(@Nullable Resource resource, String jcrPropNameForSxType) {
    return Optional.ofNullable(resource)
        .map(res -> res.adaptTo(ValueMap.class))
        .map(valueMap -> valueMap.get(jcrPropNameForSxType, String.class))
        .map(sxType -> Map.of(SX_TYPE, sxType))
        .orElseGet(Collections::emptyMap);
  }
}
