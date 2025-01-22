package dev.streamx.aem.connector.blueprints;

import eu.ciechanowiec.sneakyfun.SneakyFunction;
import java.util.Optional;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.engine.SlingRequestProcessor;
import org.apache.sling.servlethelpers.internalrequests.InternalRequest;
import org.apache.sling.servlethelpers.internalrequests.SlingInternalRequest;

@Slf4j
class JSONableNode {

  private final String pathToJCRNode;
  private final ResourceResolverFactory rrFactory;
  private final SlingRequestProcessor slingRequestProcessor;

  JSONableNode(
      String pathToJCRNode, ResourceResolverFactory rrFactory,
      SlingRequestProcessor slingRequestProcessor
  ) {
    this.pathToJCRNode = pathToJCRNode;
    this.rrFactory = rrFactory;
    this.slingRequestProcessor = slingRequestProcessor;
  }

  @SneakyThrows
  @SuppressWarnings({"squid:S1874", "deprecation"})
  String json() {
    log.trace("Rendering '{}' as JSON", pathToJCRNode);
    try (ResourceResolver resourceResolver = rrFactory.getAdministrativeResourceResolver(null)) {
      return Optional.ofNullable(resourceResolver.getResource(pathToJCRNode))
          .map(Resource::getPath)
          .map(
              resourcePath -> new SlingInternalRequest(
                  resourceResolver, slingRequestProcessor, resourcePath
              )
          ).map(slingInternalRequest -> slingInternalRequest.withExtension("json"))
          .map(slingInternalRequest -> slingInternalRequest.withSelectors("infinity"))
          .map(SneakyFunction.sneaky(InternalRequest::execute))
          .map(SneakyFunction.sneaky(InternalRequest::getResponseAsString))
          .orElse("{ }");
    }
  }
}
