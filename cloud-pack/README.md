# Cloud Package with AEM Connector

## Overview

This directory provides an uber [Apache Jackrabbit FileVault](https://jackrabbit.apache.org/filevault/index.html) package with the Adobe Experience Manager (AEM) Connector for [StreamX](https://www.streamx.dev/StreamX) (*"Cloud Package with AEM Connector"*). The term _uber_ signifies that the package includes all necessary dependencies and OSGi configurations required for the AEM Connector to function within AEM.

The Cloud Package with AEM Connector is compatible with both AEM as a Cloud Service and AEM 6.5.22+.

For AEM 6.5 versions lower than 6.5.22, installation of AEM Connector can be achieved in two ways:
1. Ensure the `org.apache.felix:org.apache.felix.configadmin:1.9.12+` bundle is installed in AEM and then install there the Cloud Package with AEM Connector.
1. Create and install in AEM all necessary dependencies and OSGi configurations required for the AEM Connector to function within AEM, in a way similar how this is made in this Cloud Package with AEM Connector.

## Purpose of the Cloud Package with AEM Connector

For the AEM Connector to function correctly, it requires multiple dependencies and OSGi configurations to be deployed in AEM. The Cloud Package with AEM Connector bundles all these dependencies and configurations, making it the only artifact required for a complete installation.

The Cloud Package with AEM Connector is intended to be used for basic StreamX integrations. For more advanced use cases, alternative installation options should be considered.

## Configuration

The Cloud Package with AEM Connector includes default OSGi configurations for the AEM Connector.

1. All configurations provided in the package can be modified via environment variables.
2. One specific configuration _must_ be modified via an environment variable, while the rest are _optional_ and can be adjusted if necessary. Details of these modifications are outlined below.

### Mandatory Configuration Change

The Cloud Package with AEM Connector includes an OSGi configuration `dev.streamx.sling.connector.impl.StreamxClientConfigImpl~streamx-instance`. This configuration:
1. Defines the URL for the StreamX ingestion HTTP endpoint. The deployer _must_ specify this URL by setting the `STREAMX_CLIENT_URL` environment variable for the target AEM environment.
2. Defines the JWT authentication token for the StreamX ingestion HTTP endpoint. If the endpoint requires the authentication, the deployer _must_ specify the token by setting the `STREAMX_CLIENT_AUTH_TOKEN` secret for the target AEM environment.

> **Tip:** Refer to the [Adobe documentation](https://experienceleague.adobe.com/en/docs/experience-manager-cloud-service/content/implementing/using-cloud-manager/environment-variables) to learn how to configure environment variables in AEM as a Cloud Service.

> **Warning:** The AEM Connector tracks shared assets that have already been published to StreamX, preventing them from being republished unnecessarily.  
> If the Connector is reconfigured to publish to a different StreamX instance, it's recommended to clear this tracking data by deleting the following nodes from the JCR:
> - /var/streamx/connector/sling/referenced-related-resources
> - /var/streamx/connector/sling/related-resources

### Optional Configuration Changes

The following OSGi configurations, included in the Cloud Package with AEM Connector, _can_ be adjusted via environment variables if required. Consult the respective OSGi configuration documentation for more details on each setting.

| Factory PID/PID                                                                         | Property name in OSGi configuration  | Name of corresponding environment variable | Default value set by the Cloud Package with AEM Connector                                                                  |
|-----------------------------------------------------------------------------------------|--------------------------------------|--------------------------------------------|----------------------------------------------------------------------------------------------------------------------------|
| `dev.streamx.aem.connector.blueprints.AssetPublicationHandler`                          | `assets.path.regexp`                 | `STREAMX_ASSETS_PATH_REGEXP`               | `^/content/dam/.+`                                                                                                         |
| `dev.streamx.aem.connector.blueprints.PageDataService`                                  | `pages.path.regexp`                  | `STREAMX_PAGES_PATH_REGEXP`                | `^/content/.+`                                                                                                             |
| `dev.streamx.aem.connector.blueprints.PageDataService`                                  | `templates.path.regexp`              | `STREAMX_TEMPLATES_PATH_REGEXP`            | `^/content/experience-fragments/templates/.+`                                                                              |
| `dev.streamx.aem.connector.blueprints.PageDataService`                                  | `shorten.content.paths`              | `STREAMX_SHORTEN_CONTENT_PATHS`            | `false`                                                                                                                    |
| `dev.streamx.sling.connector.impl.DefaultHttpClientFactory`                             | `insecure`                           | `STREAMX_CLIENT_INSECURE`                  | `false`                                                                                                                    |
| `dev.streamx.sling.connector.selectors.content.ResourceContentRelatedResourcesSelector` | `references.search-regexes`          | `STREAMX_REFERENCES_SEARCH_REGEXES`        | `(/content[^\"'\\s]*\\.coreimg\\.[^\"'\\s]*),(/[^\"'\\s]*etc\\.clientlibs[^\"'\\)\\s]*),url\\([\"']?([^\\)\"']+)[\"']?\\)` |
| `org.apache.sling.commons.log.LogManager.factory.config`                                | `org.apache.sling.commons.log.level` | `STREAMX_LOG_LEVEL`                        | `WARN`                                                                                                                     |

## Installation

The Cloud Package with AEM Connector can be installed using any standard method for deploying Apache Jackrabbit FileVault packages. The following sections provide installation examples for different AEM setups.

The Maven coordinates for the installation artifact are:

```xml
<dependency>
    <groupId>dev.streamx</groupId>
    <artifactId>streamx-connector-aem-cloud-pack-all</artifactId>
    <version>0.0.17</version>
    <type>zip</type>
</dependency>
```

> **Warning:** Installing the Cloud Package with AEM Connector alone is insufficient for the AEM Connector to function correctly. See [Mandatory Configuration Change](#mandatory-configuration-change) for necessary configuration changes.

### Embedding in the `all` Deployment Artifact of an AEM Project

One method for installing the Cloud Package with AEM Connector is embedding it in the `all` deployment artifact of a standard AEM project. The following steps outline how to do this for a new AEM project based on the [AEM Archetype](https://github.com/adobe/aem-project-archetype). For existing projects, adapt the steps accordingly.

1. Generate a new AEM project using the AEM Archetype:

    ```bash
    mvn -B org.apache.maven.plugins:maven-archetype-plugin:3.2.1:generate \
        -D archetypeGroupId=com.adobe.aem \
        -D archetypeArtifactId=aem-project-archetype \
        -D archetypeVersion=51 \
        -D appTitle="First Hops" \
        -D appId="firsthops" \
        -D groupId="dev.streamx" \
        -D artifactId="firsthops" \
        -D package="dev.streamx.firsthops" \
        -D singleCountry=n \
        -D includeExamples=y \
        -D version="1.0-SNAPSHOT" \
        -D aemVersion="cloud"
    ```

2. Add the Cloud Package with AEM Connector as a dependency in the `dependencies` section of `all/pom.xml` and exclude the `dev.streamx:ingestion-client` artifact:

    ```xml
    <dependencies>
        ...
        <dependency>
            <groupId>dev.streamx</groupId>
            <artifactId>streamx-connector-aem-cloud-pack-all</artifactId>
            <version>0.0.17</version>
            <type>zip</type>
            <exclusions>
                <exclusion>
                    <groupId>dev.streamx</groupId>
                    <artifactId>ingestion-client</artifactId>
                </exclusion>
            </exclusions>
        </dependency>
        ...
    </dependencies>
    ```

3. Configure the `filevault-package-maven-plugin` in `all/pom.xml` to embed the Cloud Package with AEM Connector:

    ```xml
    <build>
        <plugins>
            ...
            <plugin>
                <groupId>org.apache.jackrabbit</groupId>
                <artifactId>filevault-package-maven-plugin</artifactId>
                <extensions>true</extensions>
                <configuration>
                    ...
                    <embeddeds>
                        ...
                        <embedded>
                            <groupId>dev.streamx</groupId>
                            <artifactId>streamx-connector-aem-cloud-pack-all</artifactId>
                            <type>zip</type>
                            <target>/apps/firsthops-vendor-packages/content/install</target>
                        </embedded>
                        ...
                    </embeddeds>
                    ...
                </configuration>
            </plugin>
            ...
        </plugins>
    </build>
    ```

4. Deploy the `all` deployment artifact to a running AEM instance. Once deployed, the Cloud Package with AEM Connector will be installed.

### Deploying as a Separate Artifact

Another approach is deploying the Cloud Package with AEM Connector as a standalone artifact.

#### Deploying to a Local AEM Author Instance

To deploy the Cloud Package with AEM Connector to a running local AEM Author instance, run the following command in this directory:

```bash
mvn clean install -PautoInstallSinglePackage
```

#### Deploying to a Rapid Development Environment (RDE)

1. To deploy the Cloud Package with AEM Connector to a running AEM Author instance in the Rapid Development Environment (RDE), first build the package:

    ```bash
    mvn clean package
    ```

   This will create an artifact at `cloud-pack.all/target/streamx-connector-aem-cloud-pack-all-0.0.17`.

2. Deploy the artifact to an AEM Author instance in RDE using [AIO CLI](https://experienceleague.adobe.com/en/docs/experience-manager-learn/cloud-service/local-development-environment-set-up/development-tools#aio-cli). Replace `<your-programId>` and `<your-environmentId>` with the relevant values:

    ```bash
    aio aem:rde:install cloud-pack.all/target/streamx-connector-aem-cloud-pack-all-0.0.17.zip --programId <your-programId> --environmentId <your-environmentId>
    ```
