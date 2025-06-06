package dev.streamx.aem.connector.test.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;
import java.util.Map;
import java.util.Random;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.engine.SlingRequestProcessor;

public class RandomBytesSlingRequestProcessor implements SlingRequestProcessor {

  private final Map<String, Integer> requestUriToResponseBytesCountMap;

  public RandomBytesSlingRequestProcessor(String requestUriToHandle, int responseBytesCount) {
    this.requestUriToResponseBytesCountMap = Collections.singletonMap(requestUriToHandle, responseBytesCount);
  }

  public RandomBytesSlingRequestProcessor(Map<String, Integer> requestUriToResponseBytesCountMap) {
    this.requestUriToResponseBytesCountMap = Collections.unmodifiableMap(requestUriToResponseBytesCountMap);
  }

  @Override
  public void processRequest(HttpServletRequest request, HttpServletResponse response, ResourceResolver resolver) throws IOException {
    String requestURI = request.getRequestURI();
    assertThat(requestUriToResponseBytesCountMap).containsKey(requestURI);
    Integer responseBytesCount = requestUriToResponseBytesCountMap.get(requestURI);
    writeRandomBytes(response, responseBytesCount);
  }

  private static void writeRandomBytes(HttpServletResponse response, int bytesCount) throws IOException {
    response.setContentType("application/octet-stream");
    response.setContentLength(bytesCount);
    byte[] randomData = new byte[bytesCount];
    new Random(0L).nextBytes(randomData);
    try (OutputStream out = response.getOutputStream()) {
      out.write(randomData);
      out.flush();
    }
  }
}
