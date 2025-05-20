package dev.streamx.aem.connector.blueprints;

import dev.streamx.blueprints.data.Fragment;
import dev.streamx.sling.connector.PublicationHandler;
import dev.streamx.sling.connector.PublishData;
import dev.streamx.sling.connector.StreamxPublicationException;
import dev.streamx.sling.connector.UnpublishData;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.commons.io.IOUtils;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.uri.SlingUri;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.propertytypes.ServiceDescription;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(service = PublicationHandler.class)
@Designate(ocd = FragmentPublicationHandlerConfig.class)
@ServiceDescription("Publication handler for Experience Fragments")
public class FragmentPublicationHandler implements PublicationHandler<Fragment> {

  private static final Logger LOG = LoggerFactory.getLogger(FragmentPublicationHandler.class);

  private final PageDataService pageDataService;
  private final ResourceResolverFactory resourceResolverFactory;
  private final AtomicReference<FragmentPublicationHandlerConfig> config;

  @Activate
  public FragmentPublicationHandler(
      @Reference(cardinality = ReferenceCardinality.MANDATORY)
      PageDataService pageDataService,
      @Reference(cardinality = ReferenceCardinality.MANDATORY)
      ResourceResolverFactory resourceResolverFactory,
      FragmentPublicationHandlerConfig config
  ) {
    this.pageDataService = pageDataService;
    this.resourceResolverFactory = resourceResolverFactory;
    this.config = new AtomicReference<>(config);
  }

  @Modified
  void configure(FragmentPublicationHandlerConfig config) {
    this.config.set(config);
  }

  @Override
  public String getId() {
    return this.getClass().getSimpleName();
  }

  @Override
  public boolean canHandle(String resourcePath) {
    boolean canHandle = config.get().enabled() && isXF(resourcePath);
    LOG.trace("Can handle {}? Answer: {}", resourcePath, canHandle);
    return canHandle;
  }

  private boolean isXF(String resourcePath) {
    SlingUri slingUri = new DefaultSlingUriBuilder(resourcePath, resourceResolverFactory).build();
    boolean isXF = ResourcePrimaryNodeTypeChecker.isXF(slingUri, resourceResolverFactory);
    LOG.trace("Is {} an XF? Answer: {}", slingUri, isXF);
    return isXF;
  }

  @Override
  @SuppressWarnings({"squid:S1874", "deprecation"})
  public PublishData<Fragment> getPublishData(String resourcePath)
      throws StreamxPublicationException {
    try (
        ResourceResolver resourceResolver
            = resourceResolverFactory.getAdministrativeResourceResolver(null)
    ) {
      return Optional.of(resourceResolver.resolve(resourcePath))
          .filter(resolvedResource -> !ResourceUtil.isNonExistingResource(resolvedResource))
          .map(this::toPublishData)
          .orElseThrow(() -> new IllegalArgumentException("Resource not found: " + resourcePath));
    } catch (LoginException exception) {
      String message = String.format("Cannot generate PublishData for %s", resourcePath);
      throw new StreamxPublicationException(message, exception);
    }
  }

  @Override
  public UnpublishData<Fragment> getUnpublishData(String resourcePath) {
    return new UnpublishData<>(
        toStreamXKey(resourcePath),
        config.get().publication_channel(),
        Fragment.class
    );
  }

  private static String toStreamXKey(String resourcePath) {
    return String.format("%s.html", resourcePath);
  }

  private PublishData<Fragment> toPublishData(Resource resource) {
    Map<String, String> messageProps = Optional.ofNullable(
            resource.getChild(
                config.get().rel$_$path$_$to$_$node$_$with$_$jcr$_$prop$_$for$_$sx$_$type()
            )
        ).map(child -> child.adaptTo(ValueMap.class))
        .map(
            valueMap -> valueMap.get(
                config.get().jcr$_$prop$_$name_for$_$sx$_$type(), String.class
            )
        ).map(propertyValue -> Map.of(SXType.VALUE, propertyValue))
        .orElse(Map.of());
    return new PublishData<>(
        toStreamXKey(resource.getPath()),
        config.get().publication_channel(),
        Fragment.class,
        toFragment(resource),
        messageProps
    );
  }

  private Fragment toFragment(Resource resource) {
    try {
      InputStream inputStream = pageDataService.getStorageData(resource);
      return new Fragment(ByteBuffer.wrap(IOUtils.toByteArray(inputStream)));
    } catch (IOException exception) {
      String message = String.format("Cannot create fragment for %s", resource);
      throw new UncheckedIOException(message, exception);
    }
  }
}
