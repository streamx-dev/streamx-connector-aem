package dev.streamx.aem.connector.impl;

import com.day.cq.replication.ReplicationAction;
import com.day.cq.replication.ReplicationActionType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.osgi.service.event.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class ReplicationEvents {

  private static final Logger LOG = LoggerFactory.getLogger(ReplicationEvents.class);
  private final Event event;

  ReplicationEvents(Event event) {
    this.event = event;
  }

  List<ReplicationEvent> get() {
    return fromReplicationAction()
        .map(List::of)
        .orElseGet(this::fromProps);
  }

  private Optional<ReplicationEvent> fromReplicationAction() {
    LOG.trace(
        "Converting {} to {} from {}", event, ReplicationEvent.class, ReplicationAction.class
    );
    return Optional.ofNullable(ReplicationAction.fromEvent(event))
        .map(
            action -> {
              ReplicationActionType replicationActionType = action.getType();
              List<String> paths = List.of(action.getPaths());
              return new ReplicationEvent(replicationActionType, paths);
            }
        );
  }

  private List<ReplicationEvent> fromProps() {
    LOG.trace("Converting {} to {} from props", event, ReplicationEvent.class);
    return (List<ReplicationEvent>) Optional.ofNullable(
            event.getProperty("modifications")
        ).filter(ArrayList.class::isInstance)
        .map(ArrayList.class::cast)
        .map(ArrayList::stream)
        .orElse(Stream.empty())
        .filter(HashMap.class::isInstance)
        .filter(
            modProps -> ((Map<Object, Object>) modProps).keySet()
                .stream()
                .allMatch(String.class::isInstance)
        ).map(modProps -> toReplicationEvent((Map<String, Object>) modProps))
        .flatMap(modEventOptional -> ((Optional<ReplicationEvent>) modEventOptional).stream())
        .collect(Collectors.toUnmodifiableList());
  }

  private Optional<ReplicationEvent> toReplicationEvent(Map<String, Object> props) {
    LOG.trace("Converting {} to {}", props, ReplicationEvent.class);
    return Optional.ofNullable(props.get("paths"))
        .filter(String[].class::isInstance)
        .map(String[].class::cast)
        .map(List::of)
        .flatMap(
            paths ->
                Optional.ofNullable(props.get("type"))
                    .filter(ReplicationActionType.class::isInstance)
                    .map(ReplicationActionType.class::cast)
                    .map(
                        replicationActionType -> new ReplicationEvent(replicationActionType, paths)
                    )
        );
  }
}
