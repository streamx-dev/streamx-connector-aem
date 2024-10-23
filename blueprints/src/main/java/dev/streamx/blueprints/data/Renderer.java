package dev.streamx.blueprints.data;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.nio.ByteBuffer;

/**
 * Represents object containing information how to render data. See {@link RenderingContext}.
 */
public class Renderer {

  private ByteBuffer template;

  public Renderer() {
  }

  public Renderer(ByteBuffer template) {
    this.template = template;
  }

  public Renderer(byte[] template) {
    this(ByteBuffer.wrap(template));
  }

  public Renderer(String content) {
    this(content.getBytes(UTF_8));
  }

  public ByteBuffer getTemplate() {
    return template;
  }

  public void setTemplate(ByteBuffer template) {
    this.template = template;
  }

}
