package dev.streamx.aem.connector.blueprints;

import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.uri.SlingUri;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class XFCandidate {

  private static final Logger LOG = LoggerFactory.getLogger(XFCandidate.class);

  private final ResourceResolverFactory resourceResolverFactory;
  private final SlingUri slingUri;

  XFCandidate(
      ResourceResolverFactory resourceResolverFactory,
      SlingUri slingUri
  ) {
    this.resourceResolverFactory = resourceResolverFactory;
    this.slingUri = slingUri;
  }

  boolean isXF() {
    boolean isPage = new PageCandidate(
        resourceResolverFactory,
        slingUri,
        ".*"
    ).isPage();
    String resourcePath = Optional.ofNullable(slingUri.getResourcePath()).orElse(StringUtils.EMPTY);
    boolean isXFPath = resourcePath.startsWith("/content/experience-fragments");
    boolean isXF = isPage && isXFPath;
    LOG.trace(
        "Is {} an XF? Answer: {}. Is Page: {}. Is XF path: {}",
        slingUri, isXF, isPage, isXFPath
    );
    return isXF;
  }

}
