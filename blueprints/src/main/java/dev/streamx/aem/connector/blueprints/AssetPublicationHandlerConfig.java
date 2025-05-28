package dev.streamx.aem.connector.blueprints;

import org.apache.commons.lang3.StringUtils;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(name = "StreamX Blueprints - Asset Publication Handler Config")
public @interface AssetPublicationHandlerConfig {

  @AttributeDefinition(
      name = "Is Enabled?",
      description = "Indicates whether the configured service is enabled "
          + "and must undertake the supposed actions or not.",
      type = AttributeType.BOOLEAN,
      defaultValue = "true"
  )
  boolean enabled() default true;

  @AttributeDefinition(
      name = "Assets Path - RexExp",
      description = "RegExp to match paths of assets that should be published to StreamX.",
      type = AttributeType.STRING,
      defaultValue = "^/content/dam/.*$"
  )
  String assets_path_regexp() default "^/content/dam/.*$";

  @AttributeDefinition(
      name = "Publications Channel Name",
      description = "Name of the channel in StreamX to publish assets to.",
      type = AttributeType.STRING,
      defaultValue = "assets"
  )
  String publication_channel() default "assets";

  @AttributeDefinition(
      name = "Name of JCR property for `sx:type`",
      description = "If the resource that is being published has a JCR property with the specified "
          + "name, the value of that property will be set as a value of the `sx:type` "
          + "property of the message ingested into StreamX.",
      type = AttributeType.STRING,
      defaultValue = StringUtils.EMPTY
  )
  String jcr_prop_name_for_sx_type() default StringUtils.EMPTY;
}
