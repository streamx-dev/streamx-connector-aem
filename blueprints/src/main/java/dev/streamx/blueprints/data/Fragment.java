package dev.streamx.blueprints.data;

import java.nio.ByteBuffer;

/**
 * Represents fragment of web content. Can be included on {@link Page}
 */
public class Fragment extends WebResource {

  private Fragment() {
    // needed for Avro serialization
  }

  public Fragment(ByteBuffer content) {
    super(content);
  }

  public Fragment(String content) {
    super(content);
  }
}
