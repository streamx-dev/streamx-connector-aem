package dev.streamx.aem.connector.blueprints;

import com.adobe.aemds.guide.utils.JcrResourceConstants;
import dev.streamx.blueprints.data.Fragment;
import dev.streamx.blueprints.data.Page;
import dev.streamx.sling.connector.PublicationHandler;
import dev.streamx.sling.connector.PublishData;
import dev.streamx.sling.connector.UnpublishData;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeTypeDefinition;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
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
@Designate(ocd = PagePublicationHandlerConfig.class)
@ServiceDescription("Publication handler for AEM Pages")
public class PagePublicationHandler<T extends Page> implements PublicationHandler<T> {

  private static final Logger LOG = LoggerFactory.getLogger(PagePublicationHandler.class);

  private final PageDataService pageDataService;
  private final ResourceResolverFactory resolverFactory;
  private final AtomicReference<Collection<Mapping>> pagesRegexpsToChannels;
  private final AtomicBoolean enabled;

  @Activate
  public PagePublicationHandler(
      @Reference(cardinality = ReferenceCardinality.MANDATORY)
      PageDataService pageDataService,
      @Reference(cardinality = ReferenceCardinality.MANDATORY)
      ResourceResolverFactory resolverFactory,
      PagePublicationHandlerConfig config
  ) {
    this.pageDataService = pageDataService;
    this.resolverFactory = resolverFactory;
    this.pagesRegexpsToChannels = new AtomicReference<>(List.of());
    this.enabled = new AtomicBoolean(config.enabled());
    configure(config);
  }

  @Modified
  void configure(PagePublicationHandlerConfig config) {
    LOG.debug("Configuration started");
    List<Mapping> mappings = Stream.of(config.pages_path_regexp_to_channel$_$mappings())
        .map(this::splitRawMapping)
        .flatMap(Optional::stream)
        .collect(Collectors.toUnmodifiableList());
    pagesRegexpsToChannels.set(mappings);
    enabled.set(config.enabled());
    LOG.debug(
        "Configuration finished. Enabled: {}. Mappings: {}",
        enabled.get(), pagesRegexpsToChannels.get()
    );
  }

  private Optional<Mapping> splitRawMapping(String rawMapping) {
    String delimiter = "###";
    String[] splitRawMapping = rawMapping.split(delimiter);
    boolean thereAreThreeTokens = splitRawMapping.length == 3;
    if (thereAreThreeTokens) {
      Mapping mapping = new Mapping(
          splitRawMapping[NumberUtils.INTEGER_ZERO],
          splitRawMapping[NumberUtils.INTEGER_ONE],
          BooleanUtils.toBoolean(splitRawMapping[NumberUtils.INTEGER_TWO])
      );
      LOG.debug("'{}' was created out of '{}' raw mapping", mapping, rawMapping);
      return Optional.of(mapping);
    } else {
      LOG.warn("Unable to parse this mapping: {}", rawMapping);
      return Optional.empty();
    }
  }

  private boolean isExperienceFragment(Resource pageResource) {
    String pagePath = pageResource.getPath();
    return isExperienceFragment(pagePath);
  }

  private boolean isExperienceFragment(String pagePath) {
    boolean isExperienceFragment = pagesRegexpsToChannels.get().stream()
        .filter(
            mapping -> {
              String pagePathRegExp = mapping.pagePathRegExp();
              return pagePath.matches(pagePathRegExp);
            }
        ).map(Mapping::isExperienceFragment)
        .findFirst()
        .orElse(false);
    LOG.trace("Is '{}' an Experience Fragment? Answer: {}", pagePath, isExperienceFragment);
    return isExperienceFragment;
  }

  @Override
  public String getId() {
    return "streamx-page";
  }

  @Override
  public boolean canHandle(String resourcePath) {
    boolean canHandle =
        enabled.get() && isAEMPage(resourcePath) && isServicedPagePath(resourcePath);
    LOG.trace("Can handle '{}'? Answer: {}", resourcePath, canHandle);
    return canHandle;
  }

  private boolean isAEMPage(String resourcePath) {
    try (ResourceResolver resourceResolver = createResourceResolver()) {
      boolean isAEMPage = Optional.ofNullable(resourceResolver.getResource(resourcePath))
          .map(resource -> resource.adaptTo(Node.class))
          .flatMap(
              node -> {
                try {
                  return Optional.of(node.getPrimaryNodeType());
                } catch (RepositoryException exception) {
                  String message = String.format(
                      "Cannot get primary node type of '%s'",
                      resourcePath
                  );
                  LOG.error(message, exception);
                  return Optional.empty();
                }
              }
          ).map(NodeTypeDefinition::getName)
          .stream()
          .anyMatch(primaryType -> primaryType.equals(JcrResourceConstants.CQ_PAGE));
      LOG.trace("Is '{}' an AEM page? Answer: {}", resourcePath, isAEMPage);
      return isAEMPage;
    }
  }

  private boolean isServicedPagePath(String pagePath) {
    return pagesRegexpsToChannels.get().stream()
        .map(Mapping::pagePathRegExp)
        .anyMatch(pagePath::matches);
  }

  private Collection<String> matchChannels(String pagePath) {
    List<String> matchingChannels = pagesRegexpsToChannels.get().stream()
        .filter(
            mapping -> {
              String regex = mapping.pagePathRegExp();
              return pagePath.matches(regex);
            }
        ).map(Mapping::channelName)
        .collect(Collectors.toUnmodifiableList());
    LOG.trace("Matching channels for '{}': {}", pagePath, matchingChannels);
    return matchingChannels;
  }

  @Override
  public PublishData<T> getPublishData(String resourcePath) {
    try (ResourceResolver resourceResolver = createResourceResolver()) {
      Resource resource = resourceResolver.getResource(resourcePath);

      if (resource == null) {
        LOG.error("Resource not found when trying to publish it: {}", resourcePath);
        return null;
      }

      return new PublishData<>(
          getPagePath(resourcePath),
          matchChannels(resourcePath).stream().findFirst().orElseThrow(),
          isExperienceFragment(resource) ? (Class<T>) Fragment.class : (Class<T>) Page.class,
          getPageModel(resource));
    }
  }

  @Override
  public UnpublishData<T> getUnpublishData(String resourcePath) {
    return new UnpublishData<>(
        getPagePath(resourcePath),
        matchChannels(resourcePath).stream().findFirst().orElseThrow(),
        isExperienceFragment(resourcePath) ? (Class<T>) Fragment.class : (Class<T>) Page.class
    );
  }

  private static String getPagePath(String resourcePath) {
    return resourcePath + ".html";
  }

  private T getPageModel(Resource resource) {
    try {
      InputStream inputStream = pageDataService.getStorageData(resource);
      if (isExperienceFragment(resource)) {
        return (T) new Fragment(ByteBuffer.wrap(IOUtils.toByteArray(inputStream)));
      } else {
        return (T) new Page(ByteBuffer.wrap(IOUtils.toByteArray(inputStream)));
      }
    } catch (IOException e) {
      throw new UncheckedIOException("Cannot create page model", e);
    }
  }

  private ResourceResolver createResourceResolver() {
    try {
      return resolverFactory.getAdministrativeResourceResolver(null);
    } catch (LoginException e) {
      throw new IllegalStateException("Cannot create ResourceResolver", e);
    }
  }

}
