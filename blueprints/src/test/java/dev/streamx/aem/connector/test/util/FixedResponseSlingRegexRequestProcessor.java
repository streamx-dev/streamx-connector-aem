package dev.streamx.aem.connector.test.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.regex.Pattern;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.engine.SlingRequestProcessor;

public class FixedResponseSlingRegexRequestProcessor implements SlingRequestProcessor {

  private final Pattern requestUriPattern;
  private final String responseString;

  public FixedResponseSlingRegexRequestProcessor(String requestUriPattern, String responseString) {
    this.requestUriPattern = Pattern.compile(requestUriPattern);
    this.responseString = responseString;
  }

  @Override
  public void processRequest(HttpServletRequest request, HttpServletResponse response, ResourceResolver resolver) throws IOException {
    String requestUri = request.getRequestURI();
    assertThat(requestUri).matches(requestUriPattern);
    response.getWriter().write(responseString);
  }
}
