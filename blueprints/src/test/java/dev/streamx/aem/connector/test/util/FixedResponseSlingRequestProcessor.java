package dev.streamx.aem.connector.test.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.engine.SlingRequestProcessor;

public class FixedResponseSlingRequestProcessor implements SlingRequestProcessor {

  private final Map<String, String> requestUriToResponseStringMap;

  public FixedResponseSlingRequestProcessor(Map<String, String> requestUriToResponseStringMap) {
    this.requestUriToResponseStringMap = Collections.unmodifiableMap(requestUriToResponseStringMap);
  }

  @Override
  public void processRequest(HttpServletRequest request, HttpServletResponse response, ResourceResolver resolver) throws IOException {
    String requestURI = request.getRequestURI();
    assertThat(requestUriToResponseStringMap).containsKey(requestURI);
    String responseString = requestUriToResponseStringMap.get(requestURI);
    response.getWriter().write(responseString);
  }
}
