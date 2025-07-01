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
      name = "Properties to load from JCR",
      description = "Names of JCR properties from the processed resource that should be loaded and passed to publication handlers.",
      type = AttributeType.STRING,
      defaultValue = {CONTENT_TEMPLATE, PRIMARY_TYPE}
  )
  String[] properties_to_load_from_jcr() default {CONTENT_TEMPLATE, PRIMARY_TYPE};
}
