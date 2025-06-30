package dev.streamx.blueprints.data;

/**
 * Represents configuration defining what should be rendered. Relates to {@link Renderer} and {@link Data}.
 */
public class RenderingContext {

  private String rendererKey;
  private String dataKeyMatchPattern;
  private String dataTypeMatchPattern;
  private String outputKeyTemplate;
  private String outputTypeTemplate;
  private OutputFormat outputFormat;

  public RenderingContext() {
  }

  public RenderingContext(String rendererKey, String dataKeyMatchPattern,
      String dataTypeMatchPattern, String outputKeyTemplate, String outputTypeTemplate,
      OutputFormat outputFormat) {
    this.rendererKey = rendererKey;
    this.dataKeyMatchPattern = dataKeyMatchPattern;
    this.dataTypeMatchPattern = dataTypeMatchPattern;
    this.outputKeyTemplate = outputKeyTemplate;
    this.outputTypeTemplate = outputTypeTemplate;
    this.outputFormat = outputFormat;
  }

  public String getRendererKey() {
    return rendererKey;
  }

  public String getDataKeyMatchPattern() {
    return dataKeyMatchPattern;
  }

  public String getDataTypeMatchPattern() {
    return dataTypeMatchPattern;
  }

  public String getOutputKeyTemplate() {
    return outputKeyTemplate;
  }

  public String getOutputTypeTemplate() {
    return outputTypeTemplate;
  }

  public OutputFormat getOutputFormat() {
    return outputFormat;
  }

  public enum OutputFormat {
    PAGE, FRAGMENT
  }
}
