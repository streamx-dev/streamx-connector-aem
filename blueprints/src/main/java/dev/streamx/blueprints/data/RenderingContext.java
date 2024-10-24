package dev.streamx.blueprints.data;

/**
 * Represents configuration defining what should be rendered. Relates to {@link Renderer} and data.
 */
public class RenderingContext {

  private String dataKeyMatchPattern;
  private String rendererKey;
  private String outputKeyTemplate;
  private OutputType outputType;

  public RenderingContext() {
  }

  public RenderingContext(String rendererKey, String dataKeyMatchPattern, String outputKeyTemplate,
      OutputType outputType) {
    this.rendererKey = rendererKey;
    this.dataKeyMatchPattern = dataKeyMatchPattern;
    this.outputKeyTemplate = outputKeyTemplate;
    this.outputType = outputType;
  }

  public String getRendererKey() {
    return rendererKey;
  }

  public void setRendererKey(String rendererKey) {
    this.rendererKey = rendererKey;
  }

  public String getDataKeyMatchPattern() {
    return dataKeyMatchPattern;
  }

  public void setDataKeyMatchPattern(String dataKeyMatchPattern) {
    this.dataKeyMatchPattern = dataKeyMatchPattern;
  }

  public String getOutputKeyTemplate() {
    return outputKeyTemplate;
  }

  public void setOutputKeyTemplate(String outputKeyTemplate) {
    this.outputKeyTemplate = outputKeyTemplate;
  }

  public OutputType getOutputType() {
    return outputType;
  }

  public void setOutputType(OutputType outputType) {
    this.outputType = outputType;
  }

  public enum OutputType {
    PAGE, FRAGMENT, COMPOSITION;
  }
}
