package dev.streamx.aem.connector.blueprints;

import java.io.IOException;
import java.util.Optional;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.engine.SlingRequestProcessor;
import org.apache.sling.servlethelpers.internalrequests.SlingInternalRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class JSONableNode {

  private static final Logger LOG = LoggerFactory.getLogger(JSONableNode.class);

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

  @SuppressWarnings({"squid:S1874", "deprecation"})
  String json() {
    LOG.trace("Rendering '{}' as JSON", pathToJCRNode);
    try (ResourceResolver resourceResolver = rrFactory.getAdministrativeResourceResolver(null)) {
      return Optional.ofNullable(resourceResolver.getResource(pathToJCRNode))
          .map(Resource::getPath)
          .map(
              resourcePath -> new SlingInternalRequest(
                  resourceResolver, slingRequestProcessor, resourcePath
              )
          ).map(slingInternalRequest -> slingInternalRequest.withExtension("json"))
          .map(slingInternalRequest -> slingInternalRequest.withSelectors("infinity"))
          .flatMap(
              internalRequest -> {
                try {
                  return Optional.of(internalRequest.execute());
                } catch (IOException exception) {
                  String message = String.format("Cannot execute request for '%s'", pathToJCRNode);
                  LOG.error(message, exception);
                  return Optional.empty();
                }
              }
          )
          .map(internalRequest -> {
            try {
              return internalRequest.getResponseAsString();
            } catch (IOException exception) {
              String message = String.format("Cannot get response String for '%s'", pathToJCRNode);
              LOG.error(message, exception);
              return "{ }";
            }
          })
          .orElse("{ }");
    } catch (LoginException exception) {
      String message = String.format("Cannot get JSON for '%s'", pathToJCRNode);
      LOG.error(message, exception);
      return "{ }";
    }
  }
}
