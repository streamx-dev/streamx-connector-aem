package dev.streamx.aem.connector.blueprints;

import com.day.cq.wcm.api.Page;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class FranklinCheck {

  private static final String FRANKLIN_RES_TYPE_REGEX = "core/franklin/components/page/.+";
  private static final Logger LOG = LoggerFactory.getLogger(FranklinCheck.class);

  private final Page pageToCheck;

  FranklinCheck(Page pageToCheck) {
    this.pageToCheck = pageToCheck;
  }

  boolean isFranklinPage() {
    boolean isFranklinPage = Optional.ofNullable(pageToCheck.getContentResource())
        .map(this::isFranklinResType)
        .orElse(false);
    LOG.trace("Is {} a Franklin page? Answer: {}", pageToCheck, isFranklinPage);
    return isFranklinPage;
  }

  private boolean isFranklinResType(Resource resource) {
    ResourceResolver resourceResolver = resource.getResourceResolver();
    String parentResourceType = Optional.ofNullable(
        resourceResolver.getParentResourceType(resource)
    ).orElse(StringUtils.EMPTY);
    String resourceSuperType = Optional.ofNullable(resource.getResourceSuperType())
        .orElse(StringUtils.EMPTY);
    String resourceType = resource.getResourceType();
    return parentResourceType.matches(FRANKLIN_RES_TYPE_REGEX)
        || resourceSuperType.matches(FRANKLIN_RES_TYPE_REGEX)
        || resourceType.matches(FRANKLIN_RES_TYPE_REGEX);
  }
}
