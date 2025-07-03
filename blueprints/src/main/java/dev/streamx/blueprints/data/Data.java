package dev.streamx.blueprints.data;

import java.nio.ByteBuffer;

/**
 * Represents data object.
 */
public class Data extends Resource {

  public Data() {
  }

  public Data(ByteBuffer content) {
    super(content);
  }

  public Data(byte[] content) {
    super(content);
  }

  public Data(String content) {
    super(content);
  }
}
