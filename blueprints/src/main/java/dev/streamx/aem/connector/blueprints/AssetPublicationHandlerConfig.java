package dev.streamx.aem.connector.blueprints;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(name = "StreamX Blueprints - Asset Publication Handler Config")
@interface AssetPublicationHandlerConfig {

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

}
