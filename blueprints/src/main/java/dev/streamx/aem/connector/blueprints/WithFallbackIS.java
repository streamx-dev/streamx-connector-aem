package dev.streamx.aem.connector.blueprints;

import com.day.cq.dam.api.Asset;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.engine.SlingRequestProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class WithFallbackIS {

  private static final Logger LOG = LoggerFactory.getLogger(WithFallbackIS.class);

  private final ResourceResolverFactory rrFactory;
  private final SlingRequestProcessor slingRequestProcessor;
  private final Asset originalAsset;

  WithFallbackIS(
      ResourceResolverFactory rrFactory,
      SlingRequestProcessor slingRequestProcessor, Asset originalAsset
  ) {
    this.rrFactory = rrFactory;
    this.slingRequestProcessor = slingRequestProcessor;
    this.originalAsset = originalAsset;
  }

  InputStream getStream() {
    return Optional.ofNullable(originalAsset.getOriginal())
        .flatMap(originalRendition -> Optional.ofNullable(originalRendition.getStream()))
        .orElseGet(() -> {
          LOG.debug(
              "Unable to get stream from original rendition for {}, creating JSON stream",
              originalAsset
          );
          return streamOfJSON();
        });
  }

  private InputStream streamOfJSON() {
    String assetPath = originalAsset.getPath();
    JSONableNode jsonableNode = new JSONableNode(assetPath, rrFactory, slingRequestProcessor);
    PrettyJSONableNode prettyJSONableNode = new PrettyJSONableNode(jsonableNode);
    String json = prettyJSONableNode.json();
    byte[] jsonBytes = json.getBytes(StandardCharsets.UTF_8);
    return new ByteArrayInputStream(jsonBytes);
  }
}
