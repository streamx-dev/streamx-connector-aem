package dev.streamx.blueprints.data;

import java.nio.ByteBuffer;

public class Fragment extends Page {

  private Fragment() {
  }

  public Fragment(ByteBuffer content) {
    super(content);
  }

  public Fragment(String content) {
    super(content);
  }
}
