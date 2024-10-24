package dev.streamx.blueprints.data;

import java.nio.ByteBuffer;

/**
 * Represents web resource which usually is managed via Digital Asset Management (DAM) solution and
 * is not a technical resource.
 */
public class Asset extends WebResource {

  public Asset() {
    // needed for Avro serialization
  }

  public Asset(ByteBuffer content) {
    super(content);
  }

  public Asset(byte[] content) {
    super(content);
  }

  public Asset(String content) {
    super(content);
  }

}
