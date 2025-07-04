package dev.streamx.aem.connector.impl;

import com.day.cq.wcm.api.NameConstants;
import org.apache.jackrabbit.JcrConstants;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(name = "StreamX AEM Connector - Deletion Event Handler Config")
public @interface AemDeletionEventHandlerConfig {

  String CONTENT_TEMPLATE = JcrConstants.JCR_CONTENT + "/" + NameConstants.PN_TEMPLATE;
  String PRIMARY_TYPE = JcrConstants.JCR_PRIMARYTYPE;

  @AttributeDefinition(
      name = "Resource properties to load",
      description = "Names of properties of the processed resource that should be loaded and passed to publication handlers."
                    + "Nested properties are supported.",
      type = AttributeType.STRING,
      defaultValue = {CONTENT_TEMPLATE, PRIMARY_TYPE}
  )
  String[] resource_properties_to_load() default {CONTENT_TEMPLATE, PRIMARY_TYPE};
}
