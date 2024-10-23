package dev.streamx.blueprints.data;

import java.nio.ByteBuffer;

/**
 * Represents web page.
 */
public class Page extends WebResource {

  public Page() {
  }

  public Page(ByteBuffer content) {
    super(content);
  }

  public Page(byte[] content) {
    super(content);
  }

  public Page(String content) {
    super(content);
  }
}
