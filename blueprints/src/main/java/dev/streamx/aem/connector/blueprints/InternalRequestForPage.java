package dev.streamx.aem.connector.blueprints;

import dev.streamx.sling.connector.util.SimpleInternalRequest;
import java.util.Map;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.uri.SlingUri;
import org.apache.sling.api.uri.SlingUriBuilder;
import org.apache.sling.engine.SlingRequestProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class InternalRequestForPage {

  private static final Logger LOG = LoggerFactory.getLogger(InternalRequestForPage.class);

  private InternalRequestForPage() {
    // no instances
  }

  static String generateMarkup(Resource resourceWithPage, ResourceResolver resourceResolver,
      SlingRequestProcessor slingRequestProcessor, Map<String, String> additionalProperties) {
    SlingUri slingUri = SlingUriBuilder.createFrom(resourceWithPage)
        .setSelectors(getSelectors(resourceWithPage))
        .setExtension("html")
        .build();
    String pageMarkup = new SimpleInternalRequest(
        slingUri, slingRequestProcessor, resourceResolver, additionalProperties
    ).getResponseAsString();
    LOG.debug("Generated markup for {} at {}", resourceWithPage, slingUri);
    return pageMarkup;
  }

  private static String[] getSelectors(Resource resourceWithPage) {
    return FranklinCheck.isFranklinPage(resourceWithPage)
        ? new String[]{"plain"}
        : new String[0];
  }
}
