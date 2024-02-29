package dev.streamx.aem.connector.blueprints;

import java.nio.ByteBuffer;

public class Data {

  private ByteBuffer content;

  public Data() {
  }

  public Data(ByteBuffer content) {
    this.content = content;
  }

  public ByteBuffer getContent() {
    return content;
  }

  public void setContent(ByteBuffer content) {
    this.content = content;
  }
}
