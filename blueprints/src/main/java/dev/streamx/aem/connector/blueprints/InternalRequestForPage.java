package dev.streamx.aem.connector.blueprints;

import com.day.cq.wcm.api.Page;
import dev.streamx.sling.connector.util.SimpleInternalRequest;
import java.util.Optional;
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
      SlingRequestProcessor slingRequestProcessor) {
    String[] selectors = Optional.ofNullable(resourceWithPage.adaptTo(Page.class))
        .filter(FranklinCheck::isFranklinPage)
        .map(isFranklinPage -> new String[]{"plain"})
        .orElse(new String[]{});
    SlingUri slingUri = SlingUriBuilder.createFrom(resourceWithPage)
        .setSelectors(selectors)
        .setExtension("html")
        .build();
    String pageMarkup = new SimpleInternalRequest(
        slingUri, slingRequestProcessor, resourceResolver
    ).getResponseAsString();
    LOG.debug("Generated markup for {} at {}", resourceWithPage, slingUri);
    return pageMarkup;
  }
}
