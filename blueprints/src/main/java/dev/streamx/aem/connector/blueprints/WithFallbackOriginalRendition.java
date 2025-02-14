package dev.streamx.aem.connector.blueprints;

import com.day.cq.dam.api.Asset;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.engine.SlingRequestProcessor;

class WithFallbackOriginalRendition {

  private final ResourceResolverFactory rrFactory;
  private final SlingRequestProcessor slingRequestProcessor;
  private final Asset originalAsset;

  WithFallbackOriginalRendition(
      ResourceResolverFactory rrFactory,
      SlingRequestProcessor slingRequestProcessor,
      Asset originalAsset
  ) {
    this.rrFactory = rrFactory;
    this.slingRequestProcessor = slingRequestProcessor;
    this.originalAsset = originalAsset;
  }

  WithFallbackIS originalRendition() {
    return new WithFallbackIS(rrFactory, slingRequestProcessor, originalAsset);
  }
}
