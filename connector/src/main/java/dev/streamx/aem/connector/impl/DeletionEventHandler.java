package dev.streamx.aem.connector.impl;

import dev.streamx.sling.connector.IngestedData;
import dev.streamx.sling.connector.PublicationAction;
import dev.streamx.sling.connector.StreamxPublicationService;
import dev.streamx.sling.connector.util.DefaultSlingUriBuilder;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.uri.SlingUri;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.propertytypes.ServiceRanking;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(
    service = EventHandler.class,
    immediate = true,
    property = EventConstants.EVENT_TOPIC + "=" + "com/adobe/cq/resource/delete",
    configurationPolicy = ConfigurationPolicy.OPTIONAL
)
@ServiceRanking(Integer.MAX_VALUE)
@Designate(ocd = DeletionEventHandlerConfig.class)
public class DeletionEventHandler implements EventHandler {

  private static final Logger LOG = LoggerFactory.getLogger(DeletionEventHandler.class);

  private final StreamxPublicationService streamxPublicationService;
  private final ResourceResolverFactory resourceResolverFactory;
  private final AtomicReference<DeletionEventHandlerConfig> config;

  @Activate
  public DeletionEventHandler(
      @Reference(cardinality = ReferenceCardinality.MANDATORY)
      StreamxPublicationService streamxPublicationService,
      @Reference(cardinality = ReferenceCardinality.MANDATORY)
      ResourceResolverFactory resourceResolverFactory,
      DeletionEventHandlerConfig config
  ) {
    this.streamxPublicationService = streamxPublicationService;
    this.resourceResolverFactory = resourceResolverFactory;
    this.config = new AtomicReference<>(config);
  }

  @Modified
  void configure(DeletionEventHandlerConfig config) {
    this.config.set(config);
  }

  @Override
  public void handleEvent(Event event) {
    LOG.trace("Received {}", event);
    Optional.ofNullable(event.getProperty("type"))
        .filter(String.class::isInstance)
        .map(String.class::cast)
        .filter(type -> type.equals("preDelete"))
        .map(type -> event.getProperty("path"))
        .filter(String.class::isInstance)
        .map(String.class::cast)
        .map(path -> new DefaultSlingUriBuilder(path, resourceResolverFactory).build())
        .ifPresentOrElse(
            this::ingest, () -> LOG.warn("Cannot handle {}", event)
        );
  }

  private void ingest(SlingUri slingUri) {
    LOG.trace("Ingesting {}", slingUri);
    Map<String, Object> props = extractProps(slingUri)
        .entrySet()
        .stream()
        .map(
            entry -> Map.entry(
                String.format("streamx.%s", entry.getKey()), entry.getValue()
            )
        ).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    LOG.trace("Ingesting {} with properties {}", slingUri, props);
    streamxPublicationService.ingest(
        new IngestedData() {
          @Override
          public PublicationAction ingestionAction() {
            return PublicationAction.UNPUBLISH;
          }

          @Override
          public SlingUri uriToIngest() {
            return slingUri;
          }

          @Override
          public Map<String, Object> properties() {
            return props;
          }
        }
    );
  }

  @SuppressWarnings({"squid:S1874", "deprecation"})
  private Map<String, Object> extractProps(SlingUri slingUri) {
    try (
        ResourceResolver resourceResolver
            = resourceResolverFactory.getAdministrativeResourceResolver(null)
    ) {
      Resource resource = resourceResolver.resolve(slingUri.toString());
      String propertyNameRegex = config.get().property$_$name_regex();
      return Optional.ofNullable(resource.adaptTo(ValueMap.class))
          .map(Map::entrySet)
          .orElse(Set.of())
          .stream()
          .filter(entry -> entry.getKey().matches(propertyNameRegex))
          .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    } catch (LoginException exception) {
      String message = String.format("Unable to extract primary node type from %s", slingUri);
      LOG.error(message, exception);
      return Map.of();
    }
  }

}
