package dev.streamx.aem.connector.blueprints;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(name = "StreamX Blueprints - Page Publication Handler Config")
@interface PagePublicationHandlerConfig {

  @AttributeDefinition(
      name = "Publications Channel Name",
      description = "Name of the channel in StreamX to publish pages to.",
      type = AttributeType.STRING,
      defaultValue = "pages"
  )
  String publication_channel() default "pages";

  @AttributeDefinition(
      name = "Is Enabled?",
      description = "Indicates whether the configured service is enabled "
                  + "and must undertake the supposed actions or not.",
      type = AttributeType.BOOLEAN,
      defaultValue = "true"
  )
  boolean enabled() default true;

}
