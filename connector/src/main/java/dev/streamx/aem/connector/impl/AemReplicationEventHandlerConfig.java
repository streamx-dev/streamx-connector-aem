package dev.streamx.aem.connector.impl;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.osgi.service.metatype.annotations.Option;

@ObjectClassDefinition
public @interface AemReplicationEventHandlerConfig {

  String AUTHOR_RUN_MODE = "author";
  String PUBLISH_RUN_MODE = "publish";

  @AttributeDefinition(
      name = "Push From",
      description = "Type of AEM instance (author or publish) from which "
          + "the content should be pushed to StreamX.",
      options = {
          @Option(
              label = AemReplicationEventHandlerConfig.AUTHOR_RUN_MODE,
              value = AemReplicationEventHandlerConfig.AUTHOR_RUN_MODE
          ),
          @Option(
              label = AemReplicationEventHandlerConfig.PUBLISH_RUN_MODE,
              value = AemReplicationEventHandlerConfig.PUBLISH_RUN_MODE
          ),
      },
      defaultValue = AemReplicationEventHandlerConfig.AUTHOR_RUN_MODE
  )
  String push$_$from() default AUTHOR_RUN_MODE;
}
