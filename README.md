# AEM StreamX Connector

This module gives the possibility for AEM projects to automatically publish and unpublish data
to/from StreamX during standard AEM replication.

# API Usage

This module depends on the `dev.streamx:streamx-connector-sling` module and doesn't expose its own
API.
It provides an event handler that will listen for AEM replication events and automatically make
publications to StreamX.
Clients need to provide proper configuration for the `streamx-connector-sling` module and implement
its `dev.streamx.sling.connector.PublicationHandler` interface to handle resources such as Pages and
Assets.

In current early implementation event handler listens for the `com/day/cq/replication` event, which
is fired only on Author instances.
This means that content is published to StreamX from Author only.
It should change in the future versions of the connector as ideally content should be published to
StreamX from Publish instances, not from Author instances.

# Usage:

Build

```
mvn clean install
```
