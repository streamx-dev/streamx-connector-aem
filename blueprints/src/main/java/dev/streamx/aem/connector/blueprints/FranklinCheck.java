package dev.streamx.aem.connector.blueprints;

import com.day.cq.wcm.api.Page;
import org.apache.sling.api.resource.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

class FranklinCheck {

  private static final String FRANKLIN_RES_TYPE_REGEX = "core/franklin/components/page/.+";
  private static final Logger LOG = LoggerFactory.getLogger(FranklinCheck.class);

  private final Page pageToCheck;

  FranklinCheck(Page pageToCheck) {
    this.pageToCheck = pageToCheck;
  }

  boolean isFranklinPage() {
    boolean isFranklinPage = Optional.ofNullable(pageToCheck.getContentResource())
        .map(Resource::getResourceType)
        .map(resourceType -> resourceType.matches(FRANKLIN_RES_TYPE_REGEX))
        .orElse(false);
    LOG.trace("Is {} a Franklin page? Answer: {}", pageToCheck, isFranklinPage);
    return isFranklinPage;
  }
}
