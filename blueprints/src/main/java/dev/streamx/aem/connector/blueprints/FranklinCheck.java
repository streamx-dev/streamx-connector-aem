package dev.streamx.aem.connector.blueprints;

import com.day.cq.wcm.api.Page;
import java.util.Optional;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class FranklinCheck {

  private static final Logger LOG = LoggerFactory.getLogger(FranklinCheck.class);

  private FranklinCheck() {
    // no instances
  }

  static boolean isFranklinPage(Resource resource) {
    boolean isFranklinPage = Optional.ofNullable(resource.adaptTo(Page.class))
        .map(Page::getContentResource)
        .map(FranklinCheck::isFranklinResType)
        .orElse(false);
    LOG.trace("Is {} a Franklin page? Answer: {}", resource, isFranklinPage);
    return isFranklinPage;
  }

  private static boolean isFranklinResType(Resource resource) {
    ResourceResolver resourceResolver = resource.getResourceResolver();
    return resourceResolver.isResourceType(resource, "core/franklin/components/page/v1/page");
  }
}
