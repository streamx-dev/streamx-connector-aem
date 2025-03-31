package dev.streamx.aem.connector.blueprints;

import dev.streamx.sling.connector.IngestedData;
import dev.streamx.sling.connector.PublicationAction;
import java.util.Map;
import org.apache.sling.api.uri.SlingUri;

class DefaultIngestedData implements IngestedData {

  private final SlingUri uriToIngest;

  DefaultIngestedData(SlingUri uriToIngest) {
    this.uriToIngest = uriToIngest;
  }

  @Override
  public PublicationAction ingestionAction() {
    return PublicationAction.PUBLISH;
  }

  @Override
  public SlingUri uriToIngest() {
    return uriToIngest;
  }

  @Override
  public Map<String, Object> properties() {
    return Map.of();
  }
}
