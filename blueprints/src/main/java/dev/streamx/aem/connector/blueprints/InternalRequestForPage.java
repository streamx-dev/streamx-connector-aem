package dev.streamx.aem.connector.blueprints;

import com.day.cq.wcm.api.Page;
import dev.streamx.sling.connector.SimpleInternalRequest;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.uri.SlingUri;
import org.apache.sling.api.uri.SlingUriBuilder;
import org.apache.sling.engine.SlingRequestProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class InternalRequestForPage {

  private static final Logger LOG = LoggerFactory.getLogger(InternalRequestForPage.class);

  private final ResourceResolver resourceResolver;
  private final SlingRequestProcessor slingRequestProcessor;
  private final Resource resourceWithPage;

  InternalRequestForPage(Resource resourceWithPage, SlingRequestProcessor slingRequestProcessor) {
    this.resourceResolver = resourceWithPage.getResourceResolver();
    this.slingRequestProcessor = slingRequestProcessor;
    this.resourceWithPage = resourceWithPage;
  }

  String generateMarkup() {
    String[] selectors = Optional.ofNullable(resourceWithPage.adaptTo(Page.class))
        .filter(FranklinCheck::isFranklinPage)
        .map(isFranklinPage -> new String[]{"plain"})
        .orElse(new String[]{});
    SlingUri slingUri = SlingUriBuilder.createFrom(resourceWithPage)
        .addQueryParameter("wcmmode", "disabled")
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
