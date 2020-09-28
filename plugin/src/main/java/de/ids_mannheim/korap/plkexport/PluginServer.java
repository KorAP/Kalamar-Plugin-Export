package de.ids_mannheim.korap.plkexport;

import java.io.File;
import java.util.Properties;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import org.glassfish.jersey.servlet.ServletContainer;

public class PluginServer {
    public static void main (String[] args) throws Exception {

        ServletContextHandler contextHandler = new ServletContextHandler(
            ServletContextHandler.NO_SESSIONS
            );
        contextHandler.setContextPath("/");

        Properties properties = ExWSConf.properties(null);
        
        //Default: Server is available under http://localhost:7070/
        String portStr = properties.getProperty("server.port", "7070");
        String host = properties.getProperty("server.host", "localhost");
        int port = Integer.parseInt(portStr);

        Server jettyServer = new Server();
        ServerConnector connector = new ServerConnector(jettyServer);
        connector.setPort(port);
        connector.setHost(host);
        connector.setIdleTimeout(60000);
        jettyServer.addConnector(connector);
        
        HandlerList handlers = new HandlerList();
        handlers.setHandlers(new Handler[] { contextHandler, new DefaultHandler()});
        jettyServer.setHandler(handlers);


        ServletContainer servletContainer = new ServletContainer();        
        ServletHolder servletHolder = new ServletHolder(servletContainer);
        contextHandler.addServlet(servletHolder, "/*");
        
        servletHolder.setInitOrder(0);

        // Tells the Jersey Servlet which REST service/class to load.
        servletHolder.setInitParameter(
                "jersey.config.server.provider.classnames",
                IdsExportService.class.getCanonicalName());

        try {
            jettyServer.start();
            System.out.println("PluginServer available under: http://" + host+ ":" + portStr);
            System.out.println(
                "ApiServer expected under: " +
                properties.getProperty("api.scheme") +
                "://" +
                properties.getProperty("api.host")+ ":" +
                properties.getProperty("api.port")
                );
            jettyServer.join();
        }
        finally {
            jettyServer.destroy();
        }
    }
}
