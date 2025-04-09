package dev.streamx.aem.connector.impl;

import com.day.cq.commons.Externalizer;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.osgi.service.metatype.annotations.Option;

@ObjectClassDefinition
public @interface AemReplicationEventHandlerConfig {

  @AttributeDefinition(
      name = "Push From",
      description = "Type of AEM instance (author or publish) from which "
          + "the content should be pushed to StreamX.",
      options = {
          @Option(label = Externalizer.AUTHOR, value = Externalizer.AUTHOR),
          @Option(label = Externalizer.PUBLISH, value = Externalizer.PUBLISH),
      },
      defaultValue = Externalizer.AUTHOR
  )
  String push$_$from() default Externalizer.AUTHOR;
}
