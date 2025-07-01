package dev.streamx.aem.connector.test.util;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

import io.wcm.testing.mock.aem.junit5.AemContext;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.Value;
import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;

public final class ResourceResolverFactoryMocks {

  private ResourceResolverFactoryMocks() {

  }

  /**
   * Creates mocked Resource Resolver to return the given string as Primary Node Type for any resource
   */
  public static ResourceResolverFactory withFixedResourcePrimaryNodeType(String primaryNodeType, AemContext context) throws Exception {
    Value valueMock = mock(Value.class);
    doReturn(primaryNodeType).when(valueMock).getString();

    Property propertyMock = mock(Property.class);
    doReturn(valueMock).when(propertyMock).getValue();

    Node nodeMock = mock(Node.class);
    doReturn(true).when(nodeMock).hasProperty(JcrConstants.JCR_PRIMARYTYPE);
    doReturn(propertyMock).when(nodeMock).getProperty(JcrConstants.JCR_PRIMARYTYPE);

    Resource resourceMock = mock(Resource.class);
    doReturn(nodeMock).when(resourceMock).adaptTo(Node.class);

    ResourceResolver resourceResolverMock = spy(context.resourceResolver());
    doReturn(resourceMock).when(resourceResolverMock).getResource(anyString());

    ResourceResolverFactory resourceResolverFactoryMock = mock(ResourceResolverFactory.class);
    doReturn(resourceResolverMock).when(resourceResolverFactoryMock).getAdministrativeResourceResolver(null);

    return resourceResolverFactoryMock;
  }

}
