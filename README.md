# AEM StreamX Connector

This repository contains the source code of AEM Connector. The purpose of AEM Connector is to serve as a bridge between [Adobe Experience Manager (AEM)](https://business.adobe.com/products/experience-manager/adobe-experience-manager.html) and [StreamX](https://www.streamx.dev/). AEM Connector should be installed on AEM, where it will be listening to all publish/unpublish events and respectively publish/unpublish the data to/from StreamX.

It contains two modules: `Connector` and `Blueprints`. Both of them depend on the `dev.streamx:streamx-connector-sling` module and don't expose their own API.

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

This module provides out-of-the-box `PublicationHandler` implementations. Those services perform publication of usual assets, assets embedded in page components, client libraries, pages and templates. See documentation for those services configurations for more details.

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
