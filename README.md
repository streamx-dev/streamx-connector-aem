# AEM StreamX Connector

This repository contains the source code of AEM Connector. The purpose of AEM Connector is to serve as a bridge between [Adobe Experience Manager (AEM)](https://business.adobe.com/products/experience-manager/adobe-experience-manager.html) and [StreamX](https://www.streamx.dev/). AEM Connector should be installed on AEM, where it will be listening to all publish/unpublish events and respectively publish/unpublish the data to/from StreamX.

It contains two modules: `Connector` and `Blueprints`. Both of them depend on the `dev.streamx:streamx-connector-sling` module and don't expose their own API.

# Connector

It provides an event handler that will listen for AEM replication events and automatically make
publications to StreamX.
Clients need to provide proper configuration for the `streamx-connector-sling` module and implement
its `dev.streamx.sling.connector.PublicationHandler` interface to handle resources such as Pages and
Assets (or use the ones from `Blueprints` module).

In current early implementation event handler listens for the following events which are fired only on Author instances:
 - `com/day/cq/replication`
 - `com/adobe/cq/resource/delete`

This means that content is published or unpublished to/from StreamX from Author only.
It should change in the future versions of the connector as ideally content should be published to
StreamX from Publish instances, not from Author instances.

# Blueprints

This module provides out-of-the-box `PublicationHandler` implementations. Those services perform publication of usual assets, assets embedded in page components, client libraries, pages and templates. See documentation for those services configurations for more details.

## Handling Experience Fragments
To retrieve the content of resources published to StreamX, the Blueprints' `PageDataService` issues a `SlingInternalRequest` to the running AEM instance.

If a resource being requested is a Page or an Experience Fragment (as opposed to binary resources), the Connector adds a parameter to the internal `SlingHttpServletRequest`.
The parameter is named `resolveStreamxDirectives` and has the value `"true"`.

To support conditional rendering for StreamX, users should implement a **custom Experience Fragment component** in AEM.
This customization allows the component to override the default rendering behavior and conditionally output a StreamX Include tag.

The decision logic should be based on the parameters in the incoming `SlingHttpServletRequest` (see the sample source code below).
When the `resolveStreamxDirectives` flag is set to `"true"`, you can render a special HTL expression such as:
`{{#include src="${properties.fragmentVariationPath}.html"}}`

This instructs StreamX to convert the expression into a Server Side Include (SSI), for example:
`<!--#include file="/content/experience-fragments/organization/us/en/site/footer/master.html" -->`

If your delivery environment (e.g., NGINX) is configured with `ssi on;`, the referenced fragment will be included server-side when the HTML page is generated for the end user.

An example custom Experience Fragment:

 - `.content.xml`:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<jcr:root xmlns:sling="http://sling.apache.org/jcr/sling/1.0" xmlns:cq="http://www.day.com/jcr/cq/1.0" xmlns:jcr="http://www.jcp.org/jcr/1.0"
  cq:styleElements="[div,section,article,main,aside,header,footer]"
  jcr:primaryType="cq:Component"
  jcr:title="StreamX Experience Fragment"
  sling:resourceSuperType="core/wcm/components/experiencefragment/v2/experiencefragment"
  componentGroup="StreamX Components"/>
```

 - `streamxexperiencefragment.html`:
```html
<sly data-sly-use.model="organization.core.models.StreamxExperienceFragmentModel" />

<sly data-sly-test="${model.resolveStreamxDirectives && properties.fragmentVariationPath != null}">
    {{#include src="${properties.fragmentVariationPath}.html"}}
</sly>

<sly data-sly-test="${!model.resolveStreamxDirectives}">
    <sly data-sly-resource="${resource @ resourceType='core/wcm/components/experiencefragment/v2/experiencefragment'}" />
</sly>
```

 - `StreamxExperienceFragmentModel.java`:
```java
package organization.core.models;

import java.util.Optional;
import javax.inject.Inject;
import lombok.Getter;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.models.annotations.Model;

@Model(
    adaptables = SlingHttpServletRequest.class,
    resourceType = StreamxExperienceFragmentModel.RESOURCE_TYPE
)
public class StreamxExperienceFragmentModel {

  protected static final String RESOURCE_TYPE = "organization/components/streamxexperiencefragment";

  @Getter
  private final boolean resolveStreamxDirectives;

  @Inject
  public StreamxExperienceFragmentModel(SlingHttpServletRequest request) {
    resolveStreamxDirectives = Optional
        .ofNullable(request.getRequestParameter("resolveStreamxDirectives"))
        .map(param -> "true".equals(param.getString()))
        .orElse(false);
  }
}
```

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
