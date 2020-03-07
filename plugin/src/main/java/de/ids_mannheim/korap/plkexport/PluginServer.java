package de.ids_mannheim.korap.plkexport;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

public class PluginServer {
    public static void main (String[] args) throws Exception {


        ServletContextHandler contextHandler = new ServletContextHandler(
                ServletContextHandler.NO_SESSIONS);
        contextHandler.setContextPath("/");

        Properties properties = new Properties();
        File f = new File("exportPlugin.conf");
        InputStream in = null;
        
        if (!f.exists()) {
            String rootPath = Thread.currentThread().getContextClassLoader()
                    .getResource("").getPath();
            String appConfigPath = rootPath + "exportPlugin.conf";
            in = new FileInputStream(appConfigPath);
        }
        else {
            in = new FileInputStream(f);
        }

        properties.load(in);
        in.close();
        
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
            System.out.println("PluginServer available under: http://" + host+ ":" + portStr);
            jettyServer.join();
        }
        finally {
            jettyServer.destroy();
        }
    }
}
