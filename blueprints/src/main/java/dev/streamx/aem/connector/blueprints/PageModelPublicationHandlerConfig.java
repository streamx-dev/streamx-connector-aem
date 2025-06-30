package dev.streamx.aem.connector.blueprints;

import com.day.cq.wcm.api.NameConstants;
import org.apache.jackrabbit.JcrConstants;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(name = "StreamX Blueprints - Page Model Publication Handler Config")
public @interface PageModelPublicationHandlerConfig {

  @AttributeDefinition(
      name = "Publications Channel Name",
      description = "Name of the channel in StreamX to publish page models to.",
      type = AttributeType.STRING,
      defaultValue = "data"
  )
  String publication_channel() default "data";

  @AttributeDefinition(
      name = "Is Enabled?",
      description = "Indicates whether the configured service is enabled "
          + "and must undertake the supposed actions or not.",
      type = AttributeType.BOOLEAN,
      defaultValue = "false"
  )
  boolean enabled() default false;

  @AttributeDefinition(
      name = "Page Resource Path Regex",
      description = "A regular expression used to match the resource path of pages that should be published or unpublished by this service.",
      type = AttributeType.STRING,
      defaultValue = ".*"
  )
  String page_resource_path_regex() default ".*";

  @AttributeDefinition(
      name = "Selectors to append to page resource path when retrieving page model",
      description = "One or more selector to append to page resource path when retrieving page model.",
      type = AttributeType.STRING,
      defaultValue = "model"
  )
  String[] selectors_to_append() default {"model"};

  @AttributeDefinition(
      name = "Extension to append to page resource path when retrieving page model",
      description = "Extension to append to page resource path when retrieving page model.",
      type = AttributeType.STRING,
      defaultValue = "json"
  )
  String extension_to_append() default "json";

  @AttributeDefinition(
      name = "Relative Path to Node with JCR Property for `sx:type`",
      description = "Relative path to the child node of the resource that is being published, that "
          + "is expected to have a JCR property for `sx:type`.",
      type = AttributeType.STRING,
      defaultValue = JcrConstants.JCR_PRIMARYTYPE
  )
  String rel_path_to_node_with_jcr_prop_for_sx_type() default JcrConstants.JCR_CONTENT;

  @AttributeDefinition(
      name = "Name of JCR property for `sx:type`",
      description = "If the resource that is being published has a respective child with a JCR "
          + "property with the specified name, the value of that property will be set as a "
          + "value of the `sx:type` property of the message ingested into StreamX.",
      type = AttributeType.STRING,
      defaultValue = NameConstants.NT_TEMPLATE
  )
  String jcr_prop_name_for_sx_type() default NameConstants.PN_TEMPLATE;
}
