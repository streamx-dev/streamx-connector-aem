package dev.streamx.aem.connector.blueprints;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(name = "StreamX Blueprints - Pages Data Service Config")
public @interface PageDataServiceConfig {

  @AttributeDefinition(name = "RegExp to match paths of pages that should be published to StreamX")
  String pages_path_regexp() default "^/content/[^/]+/pages/.*$";

  @AttributeDefinition(name = "RegExp to match paths of templates that should be published to StreamX")
  String templates_path_regexp() default "^/content/[^/]+/pages/templates/.*$";

  @AttributeDefinition(name = "Shorten content paths", description =
      "Shorten paths in content to not start with '/content/<space name>/pages' or '/content/<space name>'. "
          + "Shortening is unaware of the context, therefore replacing all string occurrences.")
  boolean shorten_content_paths() default true;

  @AttributeDefinition(name = "Add nofollow to external links")
  boolean nofollow_external_links() default false;

  @AttributeDefinition(name = "Skip adding nofollow to external links for hosts")
  String[] nofollow_hosts_to_skip() default {};
}
