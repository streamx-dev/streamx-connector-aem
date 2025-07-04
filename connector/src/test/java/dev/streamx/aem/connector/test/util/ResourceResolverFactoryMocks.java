package dev.streamx.aem.connector.test.util;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

import io.wcm.testing.mock.aem.junit5.AemContext;
import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.ValueMap;

public final class ResourceResolverFactoryMocks {

  private ResourceResolverFactoryMocks() {

  }

  /**
   * Creates mocked Resource Resolver to return the given string as Primary Node Type for any resource
   */
  public static ResourceResolverFactory withFixedResourcePrimaryNodeType(String primaryNodeType, AemContext context) throws Exception {
    ValueMap valueMapMock = mock(ValueMap.class);
    doReturn(primaryNodeType).when(valueMapMock).get(JcrConstants.JCR_PRIMARYTYPE, String.class);

    Resource resourceMock = mock(Resource.class);
    doReturn(valueMapMock).when(resourceMock).getValueMap();

    ResourceResolver resourceResolverMock = spy(context.resourceResolver());
    doReturn(resourceMock).when(resourceResolverMock).getResource(anyString());

    ResourceResolverFactory resourceResolverFactoryMock = mock(ResourceResolverFactory.class);
    doReturn(resourceResolverMock).when(resourceResolverFactoryMock).getAdministrativeResourceResolver(null);

    return resourceResolverFactoryMock;
  }

}
