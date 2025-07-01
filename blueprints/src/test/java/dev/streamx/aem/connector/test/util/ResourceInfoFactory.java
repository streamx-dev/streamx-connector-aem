package dev.streamx.aem.connector.test.util;

import dev.streamx.sling.connector.ResourceInfo;
import java.util.Map;
import org.apache.jackrabbit.JcrConstants;

public final class ResourceInfoFactory {

  private ResourceInfoFactory() {
    // no instances
  }

  public static ResourceInfo create(String path, String primaryNodeType) {
    return new ResourceInfo(path, Map.of(JcrConstants.JCR_PRIMARYTYPE, primaryNodeType));
  }

}
