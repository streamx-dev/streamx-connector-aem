package dev.streamx.aem.connector.blueprints;

import com.day.cq.wcm.api.Page;
import java.io.IOException;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.engine.SlingRequestProcessor;
import org.apache.sling.servlethelpers.internalrequests.SlingInternalRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class InternalRequestForPage {

  private static final Logger LOG = LoggerFactory.getLogger(InternalRequestForPage.class);

  private final ResourceResolver resourceResolver;
  private final SlingRequestProcessor slingRequestProcessor;
  private final String pathToPage;

  InternalRequestForPage(Resource resourceWithPage, SlingRequestProcessor slingRequestProcessor) {
    this.resourceResolver = resourceWithPage.getResourceResolver();
    this.slingRequestProcessor = slingRequestProcessor;
    this.pathToPage = resourceWithPage.getPath();
  }

  String generateMarkup() throws IOException {
    String selector = Optional.ofNullable(resourceResolver.getResource(pathToPage))
        .map(resourceWithPage -> resourceWithPage.adaptTo(Page.class))
        .filter(FranklinCheck::isFranklinPage)
        .map(isFranklinPage -> "plain")
        .orElse(StringUtils.EMPTY);
    String pageMarkup = new SlingInternalRequest(
        resourceResolver, slingRequestProcessor, pathToPage
    ).withSelectors(selector)
     .withExtension("html")
     .withParameter("wcmmode", "disabled")
     .execute()
     .getResponseAsString();
    LOG.debug("Generated markup for a page at path '{}'. Selector: '{}'", pathToPage, selector);
    return pageMarkup;
  }
}
