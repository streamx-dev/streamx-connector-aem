package dev.streamx.aem.connector.impl;

import dev.streamx.sling.connector.IngestedData;
import dev.streamx.sling.connector.PublicationAction;
import dev.streamx.sling.connector.StreamxPublicationService;
import dev.streamx.sling.connector.util.DefaultSlingUriBuilder;
import java.util.Map;
import java.util.Optional;
import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.uri.SlingUri;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.propertytypes.ServiceRanking;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(
    service = EventHandler.class,
    immediate = true,
    property = EventConstants.EVENT_TOPIC + "=" + "com/adobe/cq/resource/delete"
)
@ServiceRanking(Integer.MAX_VALUE)
//TODO: co wrzucamy do propsów - przez configi
public class DeletionEventHandler implements EventHandler {

  private static final Logger LOG = LoggerFactory.getLogger(DeletionEventHandler.class);

  private final StreamxPublicationService streamxPublicationService;
  private final ResourceResolverFactory resourceResolverFactory;

  @Activate
  public DeletionEventHandler(
      @Reference(cardinality = ReferenceCardinality.MANDATORY)
      StreamxPublicationService streamxPublicationService,
      @Reference(cardinality = ReferenceCardinality.MANDATORY)
      ResourceResolverFactory resourceResolverFactory
  ) {
    this.streamxPublicationService = streamxPublicationService;
    this.resourceResolverFactory = resourceResolverFactory;
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
    Map<String, Object> props = extractPrimaryNT(slingUri)
        .<Map<String, Object>>map(
            primaryNT -> Map.of("streamx." + JcrConstants.JCR_PRIMARYTYPE, primaryNT)
        ).orElse(Map.of());
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
  private Optional<String> extractPrimaryNT(SlingUri slingUri) {
    try (
        ResourceResolver resourceResolver
            = resourceResolverFactory.getAdministrativeResourceResolver(null)
    ) {
      Resource resource = resourceResolver.resolve(slingUri.toString());
      return Optional.ofNullable(resource.adaptTo(ValueMap.class))
          .map(valueMap -> valueMap.get(JcrConstants.JCR_PRIMARYTYPE, String.class));
    } catch (LoginException exception) {
      String message = String.format("Unable to extract primary node type from %s", slingUri);
      LOG.error(message, exception);
      return Optional.empty();
    }
  }

}
