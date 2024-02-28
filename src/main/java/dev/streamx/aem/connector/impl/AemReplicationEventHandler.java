package dev.streamx.aem.connector.impl;

import com.day.cq.replication.ReplicationAction;
import dev.streamx.sling.connector.StreamxPublicationException;
import dev.streamx.sling.connector.StreamxPublicationService;
import java.util.Arrays;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(
    service = EventHandler.class,
    immediate = true,
    property = {EventConstants.EVENT_TOPIC + "=" + ReplicationAction.EVENT_TOPIC}
)
public class AemReplicationEventHandler implements EventHandler {

  private static final Logger LOG = LoggerFactory.getLogger(AemReplicationEventHandler.class);

  @Reference
  private StreamxPublicationService streamxPublicationService;

  @Override
  public void handleEvent(Event event) {
    if (!streamxPublicationService.isEnabled()) {
      return;
    }

    ReplicationAction action = ReplicationAction.fromEvent(event);
    if (action == null) {
      LOG.warn("Cannot get action from replication event");
      return;
    }

    try {
      handleAction(action);
    } catch (StreamxPublicationException e) {
      LOG.error("Cannot publish to StreamX", e);
    }
  }

  private void handleAction(ReplicationAction action) throws StreamxPublicationException {
    LOG.info("Handling replication action: {} - {}", action.getType(), action.getPath());
    switch (action.getType()) {
      case ACTIVATE:
        streamxPublicationService.publish(Arrays.asList(action.getPaths()));
        break;
      case DEACTIVATE:
      case DELETE:
        streamxPublicationService.unpublish(Arrays.asList(action.getPaths()));
        break;
      default:
        LOG.debug("Unsupported replication action type: {}", action.getType());
    }
  }

}
