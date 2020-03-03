package de.ids_mannheim.korap.plkexport;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

//TODO Set port and host through configuration file
public class PluginServer {
    public static void main (String[] args) throws Exception {


        ServletContextHandler contextHandler = new ServletContextHandler(
                ServletContextHandler.NO_SESSIONS);
        contextHandler.setContextPath("/");

        Server jettyServer = new Server(7777);
        jettyServer.setHandler(contextHandler);


        ServletHolder servletHolder = contextHandler.addServlet(
                org.glassfish.jersey.servlet.ServletContainer.class, "/*");
        servletHolder.setInitOrder(0);

        // Tells the Jersey Servlet which REST service/class to load.
        servletHolder.setInitParameter(
                "jersey.config.server.provider.classnames",
                IdsExportService.class.getCanonicalName());

        try {
            jettyServer.start();
            jettyServer.join();
        }
        finally {
            jettyServer.destroy();
        }
    }
}
