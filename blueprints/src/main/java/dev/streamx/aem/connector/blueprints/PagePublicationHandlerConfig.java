package dev.streamx.aem.connector.blueprints;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(name = "StreamX Page Publication Config")
@interface PagePublicationHandlerConfig {

  @AttributeDefinition(name = "Publications channel name")
  String publication_channel() default "pages";

  @AttributeDefinition(name = "Enable page handler")
  boolean enabled() default true;

}
