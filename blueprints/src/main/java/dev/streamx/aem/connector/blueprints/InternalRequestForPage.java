package dev.streamx.aem.connector.blueprints;

import com.day.cq.wcm.api.Page;
import dev.streamx.sling.connector.SimpleInternalRequest;
import java.util.Optional;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.uri.SlingUri;
import org.apache.sling.api.uri.SlingUriBuilder;
import org.apache.sling.engine.SlingRequestProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class InternalRequestForPage {

  private static final Logger LOG = LoggerFactory.getLogger(InternalRequestForPage.class);

  private final ResourceResolverFactory resourceResolverFactory;
  private final SlingRequestProcessor slingRequestProcessor;
  private final Resource resourceWithPage;

  InternalRequestForPage(
      ResourceResolverFactory resourceResolverFactory,
      Resource resourceWithPage,
      SlingRequestProcessor slingRequestProcessor
  ) {
    this.resourceResolverFactory = resourceResolverFactory;
    this.slingRequestProcessor = slingRequestProcessor;
    this.resourceWithPage = resourceWithPage;
  }

  String generateMarkup() {
    String[] selectors = Optional.ofNullable(resourceWithPage.adaptTo(Page.class))
        .filter(FranklinCheck::isFranklinPage)
        .map(isFranklinPage -> new String[]{"plain"})
        .orElse(new String[]{});
    SlingUri slingUri = SlingUriBuilder.createFrom(resourceWithPage)
        .setSelectors(selectors)
        .setExtension("html")
        .build();
    String pageMarkup = new SimpleInternalRequest(
        slingUri, slingRequestProcessor, resourceResolverFactory
    ).getResponseAsString();
    LOG.debug("Generated markup for {} at {}", resourceWithPage, slingUri);
    return pageMarkup;
  }
}
