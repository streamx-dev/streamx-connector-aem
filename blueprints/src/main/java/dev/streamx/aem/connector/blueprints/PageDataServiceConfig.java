package dev.streamx.aem.connector.blueprints;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(name = "StreamX Blueprints - Pages Data Service Config")
public @interface PageDataServiceConfig {

  @AttributeDefinition(
      name = "Templates Path - RexExp",
      description = "RegExp to match paths of templates that should be published to StreamX.",
      type = AttributeType.STRING,
      defaultValue = "^/conf/[^/]+/templates/.*$"
  )
  String templates_path_regexp() default "^/conf/[^/]+/templates/.*$";

  @AttributeDefinition(
      name = "Shorten Content Paths",
      description = "Shorten paths in content to not start with '/content/<space name>/pages' " 
                  + "or '/content/<space name>'. Shortening is unaware of the context, therefore " 
                  + "replaces all string occurrences.",
      type = AttributeType.BOOLEAN,
      defaultValue = "true"
  )
  boolean shorten_content_paths() default true;

  @AttributeDefinition(
      name = "Add 'nofollow' to External Links",
      description = "Add 'nofollow' attribute to external links.",
      type = AttributeType.BOOLEAN,
      defaultValue = "false"
  )
  boolean nofollow_external_links() default false;

  @SuppressWarnings("DefaultAnnotationParam") 
  @AttributeDefinition(
      name = "Skip Adding 'nofollow' to External Links for Hosts",
      description = "List of hosts for which the 'nofollow' attribute should "
                  + "not be added to external links.",
      type = AttributeType.STRING,
      defaultValue = {}
  )
  String[] nofollow_hosts_to_skip() default {};
}
