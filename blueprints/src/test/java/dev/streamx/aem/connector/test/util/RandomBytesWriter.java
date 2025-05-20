package dev.streamx.aem.connector.test.util;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Random;
import javax.servlet.http.HttpServletResponse;

public class RandomBytesWriter {

  public static void writeRandomBytes(HttpServletResponse response, int count) throws IOException {
    response.setContentType("application/octet-stream");
    response.setContentLength(count);
    byte[] randomData = new byte[count];
    new Random(0L).nextBytes(randomData);
    try (OutputStream out = response.getOutputStream()) {
      out.write(randomData);
      out.flush();
    }
  }
}
