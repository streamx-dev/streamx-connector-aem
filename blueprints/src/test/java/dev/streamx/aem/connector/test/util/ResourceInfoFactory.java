package dev.streamx.aem.connector.test.util;

import static java.util.Objects.requireNonNull;

import dev.streamx.sling.connector.ResourceInfo;
import io.wcm.testing.mock.aem.junit5.AemContext;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.resource.Resource;

public final class ResourceInfoFactory {

  private ResourceInfoFactory() {
    // no instances
  }

  public static ResourceInfo page(String path) {
    return create(path, "cq:Page");
  }

  public static ResourceInfo asset(String path) {
    return create(path, "dam:Asset");
  }

  public static ResourceInfo file(String path) {
    return create(path, "nt:file");
  }

  private static ResourceInfo create(String path, String primaryNodeType) {
    return new ResourceInfo(path, Map.of(JcrConstants.JCR_PRIMARYTYPE, primaryNodeType));
  }

  @SuppressWarnings("resource")
  public static ResourceInfo createWithProperties(AemContext context, String resourcePath, String... propertyNames) throws RepositoryException {
    Resource resource = context.resourceResolver().resolve(resourcePath);
    Node node = requireNonNull(resource.adaptTo(Node.class));

    Map<String, String> properties = new LinkedHashMap<>();
    for (String propertyName : propertyNames) {
      properties.put(propertyName, node.getProperty(propertyName).getString());
    }

    return new ResourceInfo(resourcePath, properties);
  }

}
