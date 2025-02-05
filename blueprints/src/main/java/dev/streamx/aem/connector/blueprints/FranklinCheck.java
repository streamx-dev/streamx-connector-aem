package dev.streamx.aem.connector.blueprints;

import com.day.cq.wcm.api.Page;
import java.util.Optional;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class FranklinCheck {

  private static final Logger LOG = LoggerFactory.getLogger(FranklinCheck.class);

  private FranklinCheck() throws InstantiationException {
    throw new InstantiationException("Instantiation of FranklinCheck is not allowed");
  }

  static boolean isFranklinPage(Page pageToCheck) {
    boolean isFranklinPage = Optional.ofNullable(pageToCheck.getContentResource())
        .map(FranklinCheck::isFranklinResType)
        .orElse(false);
    LOG.trace("Is {} a Franklin page? Answer: {}", pageToCheck, isFranklinPage);
    return isFranklinPage;
  }

  private static boolean isFranklinResType(Resource resource) {
    ResourceResolver resourceResolver = resource.getResourceResolver();
    return resourceResolver.isResourceType(resource, "core/franklin/components/page/v1/page");
  }
}
