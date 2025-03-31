package dev.streamx.aem.connector.impl;

import org.apache.jackrabbit.JcrConstants;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition
public @interface DeletionEventHandlerConfig {

  @AttributeDefinition(
      name = "Property Name - RexExp",
      description = "This handler will pass to StreamX Ingestion Service properties of resources "
          + "that have a name that matches this regular expression.",
      type = AttributeType.STRING,
      defaultValue = JcrConstants.JCR_PRIMARYTYPE
  )
  String property$_$name_regex() default JcrConstants.JCR_PRIMARYTYPE;

}
