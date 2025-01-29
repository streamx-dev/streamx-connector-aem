package eu.ciechanowiec.firsthops.core.servlets;

import eu.ciechanowiec.sneakyfun.SneakyFunction;
import lombok.SneakyThrows;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.servlets.annotations.SlingServletPaths;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.servlet.Servlet;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;

@Component(
        service = {UniverseServlet.class, Servlet.class},
        immediate = true
)
@SlingServletPaths("/bin/universe")
public class UniverseServlet extends SlingSafeMethodsServlet {

    @Reference
    private ConfigurationAdmin configurationAdmin;

    @SneakyThrows
    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) {
        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        Integer localPort = mbs.queryNames(new ObjectName("org.eclipse.jetty.server:type=serverconnector,*"), null)
                .stream()
                .findFirst()
                .map(SneakyFunction.sneaky(objectName -> (Integer) mbs.getAttribute(objectName, "localPort")))
                .orElse(NumberUtils.INTEGER_MINUS_ONE);
        String hostAddress = InetAddress.getLocalHost().getHostAddress();
        String hostName = InetAddress.getLocalHost().getHostName();
        response.getWriter().write("Hello Universe! I'm running on " + hostName + " (" + hostAddress + ") and listening on port " + localPort);
    }
}
