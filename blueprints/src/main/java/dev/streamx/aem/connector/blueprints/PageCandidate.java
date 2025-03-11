package dev.streamx.aem.connector.blueprints;

import com.adobe.aem.formsndocuments.util.FMConstants;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.uri.SlingUri;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class PageCandidate {

  private static final Logger LOG = LoggerFactory.getLogger(PageCandidate.class);

  private final NodeTypeCheck nodeTypeCheck;
  private final SlingUri slingUri;
  private final String requiredPathRegex;

  PageCandidate(
      ResourceResolverFactory resourceResolverFactory,
      SlingUri slingUri,
      String requiredPathRegex
  ) {
    this.slingUri = slingUri;
    this.nodeTypeCheck = new NodeTypeCheck(resourceResolverFactory, slingUri);
    this.requiredPathRegex = requiredPathRegex;
  }

  boolean isPage() {
    boolean isPageNodeType = nodeTypeCheck.matches(FMConstants.CQ_PAGE_NODETYPE);
    boolean isRequiredPath = slingUri.toString().matches(requiredPathRegex);
    boolean isPage = isPageNodeType && isRequiredPath;
    LOG.trace(
        "Is {} a page? Answer: {}. Is NodeType: {}. Is required path: {}",
        slingUri, isPage, isPageNodeType, isRequiredPath
    );
    return isPage;
  }

}
