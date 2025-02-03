package dev.streamx.aem.connector.blueprints;

import com.day.cq.wcm.api.Page;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.engine.SlingRequestProcessor;
import org.apache.sling.servlethelpers.internalrequests.SlingInternalRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;

class InternalRequestForPage {

  private static final Logger LOG = LoggerFactory.getLogger(InternalRequestForPage.class);

  private final ResourceResolver resourceResolver;
  private final SlingRequestProcessor slingRequestProcessor;
  private final String pathToPage;

  InternalRequestForPage(
      ResourceResolver resourceResolver, SlingRequestProcessor slingRequestProcessor,
      String pathToPage
  ) {
    this.resourceResolver = resourceResolver;
    this.slingRequestProcessor = slingRequestProcessor;
    this.pathToPage = pathToPage;
  }

  String generateMarkup() throws IOException {
    String[] selectors = Optional.ofNullable(resourceResolver.getResource(pathToPage))
        .flatMap(resourceWithPage -> Optional.ofNullable(resourceWithPage.adaptTo(Page.class)))
        .map(FranklinCheck::new)
        .map(FranklinCheck::isFranklinPage)
        .filter(isFranklinPage -> isFranklinPage)
        .map(isFranklinPage -> new String[]{"plain"})
        .orElse(new String[]{StringUtils.EMPTY});
    String pageMarkup = new SlingInternalRequest(
        resourceResolver, slingRequestProcessor, pathToPage
    ).withSelectors(selectors)
     .withExtension("html")
     .withParameter("wcmmode", "disabled")
     .execute()
     .getResponseAsString();
    if (LOG.isDebugEnabled()) {
      LOG.debug(
          "Generated markup for a page at path '{}'. Selectors: {}", pathToPage,
          Arrays.toString(selectors)
      );
    }
    return pageMarkup;
  }
}
