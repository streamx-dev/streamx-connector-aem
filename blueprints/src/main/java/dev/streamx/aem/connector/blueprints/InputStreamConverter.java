package dev.streamx.aem.connector.blueprints;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class InputStreamConverter {

  private static final Logger LOG = LoggerFactory.getLogger(InputStreamConverter.class);

  private InputStreamConverter() {
    // no instances
  }

  static ByteBuffer toByteBuffer(InputStream inputStream) {
    try {
      byte[] bytes = IOUtils.toByteArray(inputStream);
      return ByteBuffer.wrap(bytes);
    } catch (IOException exception) {
      LOG.error("Cannot convert input stream to byte array", exception);
      return ByteBuffer.wrap(new byte[0]);
    }
  }
}
