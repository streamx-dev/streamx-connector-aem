package dev.streamx.aem.connector.impl;

import com.day.cq.replication.ReplicationActionType;
import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class ReplicationEvent {

  private static final Logger LOG = LoggerFactory.getLogger(ReplicationEvent.class);
  private final ReplicationActionType replicationActionType;
  private final List<String> paths;

  ReplicationEvent(ReplicationActionType replicationActionType, List<String> paths) {
    this.replicationActionType = replicationActionType;
    this.paths = Collections.unmodifiableList(paths);
    LOG.trace(
        "Initialized {} with {} for paths: {}", this.getClass(), replicationActionType, paths
    );
  }

  ReplicationActionType replicationActionType() {
    return replicationActionType;
  }

  List<String> paths() {
    return paths;
  }
}
