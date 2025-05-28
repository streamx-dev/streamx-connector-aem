package dev.streamx.aem.connector.test.util;


import dev.streamx.sling.connector.PublicationHandler;
import io.wcm.testing.mock.aem.junit5.AemContext;
import java.util.Map;
import org.apache.sling.testing.mock.osgi.MockOsgi;

public final class OsgiConfigUtils {

  public static void enableHandler(PublicationHandler<?> handler, AemContext context) {
    modifyEnabled(handler, context, true);
  }

  public static void disableHandler(PublicationHandler<?> handler, AemContext context) {
    modifyEnabled(handler, context, false);
  }

  private static void modifyEnabled(PublicationHandler<?> handler, AemContext context, boolean enabled) {
    MockOsgi.modified(handler, context.bundleContext(), Map.of("enabled", enabled));
  }
}
