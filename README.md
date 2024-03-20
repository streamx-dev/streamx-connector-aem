# AEM StreamX Connector

This project gives the possibility for AEM projects to automatically publish and unpublish data
to/from StreamX during standard AEM replication.
It contains two modules: `Connector` and `Blueprints`.
Both of them depend on the `dev.streamx:streamx-connector-sling` module and don't expose their own
API.

# Connector

It provides an event handler that will listen for AEM replication events and automatically make
publications to StreamX.
Clients need to provide proper configuration for the `streamx-connector-sling` module and implement
its `dev.streamx.sling.connector.PublicationHandler` interface to handle resources such as Pages and
Assets (or use the ones from `Blueprints` module).

In current early implementation event handler listens for the `com/day/cq/replication` event, which
is fired only on Author instances.
This means that content is published to StreamX from Author only.
It should change in the future versions of the connector as ideally content should be published to
StreamX from Publish instances, not from Author instances.

# Blueprints

This module provides out-of-the-box `PublicationHandler`s to handle Pages, Templates and Assets.

Pages and Templates are published not under `/content` folder but under `/published` folder.
The rest of the path remains the same.

Each handler contains its configuration. See:

- [AssetPublicationHandler](./blueprints/src/main/java/dev/streamx/aem/connector/blueprints/AssetPublicationHandlerConfig.java)
- [PagePublicationHandler](./blueprints/src/main/java/dev/streamx/aem/connector/blueprints/PagePublicationHandlerConfig.java)
  (this handler will handle both Pages and Templates)

# Usage:

To build all the modules run in the project root directory

```
mvn clean install
```

To deploy bundles to an Author instance, run

```
mvn clean install -PautoInstallBundle
```

To change port, use

```
mvn clean install -PautoInstallBundle -Daem.port=4502
```