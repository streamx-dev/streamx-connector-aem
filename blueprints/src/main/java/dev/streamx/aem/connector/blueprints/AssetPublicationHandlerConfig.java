package dev.streamx.aem.connector.blueprints;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(name = "StreamX Asset Publication Config")
@interface AssetPublicationHandlerConfig {

  @AttributeDefinition(name = "Enable asset handler")
  boolean enabled() default true;

  @AttributeDefinition(name = "RegExp to match paths of assets that should be published to StreamX")
  String assets_path_regexp() default "^/content/dam/.*$";

}
