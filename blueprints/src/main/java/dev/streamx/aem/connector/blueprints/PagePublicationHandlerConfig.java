package dev.streamx.aem.connector.blueprints;

import org.apache.commons.lang3.StringUtils;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(name = "StreamX Blueprints - Page Publication Handler Config")
public @interface PagePublicationHandlerConfig {

  @AttributeDefinition(
      name = "Page Path RegExps to Channels Mappings",
      description = "Defines a set of mappings that specify (1) a regex for page paths to be "
          + "published to StreamX, (2) the StreamX channel to publish these pages to, and "
          + "(3) a flag indicating whether the page is an Experience Fragment. "
          + "Each mapping must follow the format: "
          + "<page-path-regexp>###<channel-name>###<is-experience-fragment>, using '###' as the "
          + "delimiter. Each page path can only match one mapping.",
      defaultValue = StringUtils.EMPTY,
      type = AttributeType.STRING
  )
  @SuppressWarnings("squid:S100")
  String[] pages_path_regexp_to_channel$_$mappings() default {
      "^/content/(?!experience-fragments).*###pages###false",
      "^/content/experience-fragments/.*###fragments###true"
  };

  @AttributeDefinition(
      name = "Is Enabled?",
      description = "Indicates whether the configured service is enabled "
                  + "and must undertake the supposed actions or not.",
      type = AttributeType.BOOLEAN,
      defaultValue = "true"
  )
  boolean enabled() default true;

}
