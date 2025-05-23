package dev.streamx.aem.connector.test.util;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

import io.wcm.testing.mock.aem.junit5.AemContext;
import javax.jcr.Node;
import javax.jcr.nodetype.NodeType;
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
    NodeType nodeTypeMock = mock(NodeType.class);
    doReturn(primaryNodeType).when(nodeTypeMock).getName();

    Node nodeMock = mock(Node.class);
    doReturn(nodeTypeMock).when(nodeMock).getPrimaryNodeType();

    Resource resourceMock = mock(Resource.class);
    doReturn(nodeMock).when(resourceMock).adaptTo(Node.class);

    ResourceResolver resourceResolverMock = spy(context.resourceResolver());
    doReturn(resourceMock).when(resourceResolverMock).resolve(anyString());

    ResourceResolverFactory resourceResolverFactoryMock = mock(ResourceResolverFactory.class);
    doReturn(resourceResolverMock).when(resourceResolverFactoryMock).getAdministrativeResourceResolver(null);

    return resourceResolverFactoryMock;
  }

}
