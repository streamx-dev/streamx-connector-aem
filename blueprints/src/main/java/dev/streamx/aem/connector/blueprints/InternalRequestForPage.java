package dev.streamx.aem.connector.blueprints;

import com.drew.lang.annotations.Nullable;
import dev.streamx.sling.connector.util.SimpleInternalRequest;
import java.util.Map;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.uri.SlingUri;
import org.apache.sling.engine.SlingRequestProcessor;

final class InternalRequestForPage {

  private InternalRequestForPage() {
    // no instances
  }

  static String getHtml(Resource pageResource, ResourceResolver resourceResolver,
      SlingRequestProcessor slingRequestProcessor, Map<String, String> additionalProperties) {
    String[] selectors = FranklinCheck.isFranklinPage(pageResource) ? new String[]{"plain"} : new String[0];
    return getContent(pageResource, selectors, "html", resourceResolver, slingRequestProcessor, additionalProperties);
  }

  static String getContent(Resource pageResource,
      String[] selectors, @Nullable String extension, ResourceResolver resourceResolver,
      SlingRequestProcessor slingRequestProcessor, Map<String, String> additionalProperties) {
    SlingUri slingUri = SlingUriCreator.create(pageResource, selectors, extension);
    return new SimpleInternalRequest(
        slingUri,
        slingRequestProcessor,
        resourceResolver,
        additionalProperties
    ).getResponseAsString();
  }
}
