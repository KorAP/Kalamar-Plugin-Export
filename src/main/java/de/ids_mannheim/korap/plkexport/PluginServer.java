package de.ids_mannheim.korap.plkexport;


import java.util.Properties;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import org.glassfish.jersey.servlet.ServletContainer;

import org.tinylog.Logger;


import jakarta.servlet.Servlet;

/**
 * Server to provide the export web service
 */
public class PluginServer {
    public static void main (String[] args) throws Exception {

        ServletContextHandler contextHandler = new ServletContextHandler(
            ServletContextHandler.NO_SESSIONS
            );
        contextHandler.setContextPath("/");
        
        String propfile = null;
        String usage = "\n Usage is java -jar KalamarExportPlugin-[VERSION].jar [-h] [myconf_exportPlugin.conf]";
        boolean printhelp = false;
        boolean argexc = false;

        if(args.length >= 1 & args.length <= 2) {
            for (int i = 0; i <= args.length-1; i++) {
             if (args[i].equals("-h" ) |  args[i].equals("--help")){
              printhelp = true;
                }
              else {
                  propfile = args[i];
                }
            }
        }
        else if(args.length >= 3){
        argexc = true;
        }
        Properties properties = ExWSConf.properties(propfile);

        // Default: Server is available under http://localhost:7070/
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
        ServletHolder servletHolder = new ServletHolder((Servlet) servletContainer);
        contextHandler.addServlet(servletHolder, "/*");
        
        servletHolder.setInitOrder(0);

        // Tells the Jersey Servlet which REST service/class to load.
        servletHolder.setInitParameter(
                "jersey.config.server.provider.classnames",
                Service.class.getCanonicalName());

        try {
            jettyServer.start();
            Logger.info("PluginServer available under: http://" + host+ ":" + portStr);
            Logger.info(
                "ApiServer expected under: " +
                properties.getProperty("api.scheme") +
                "://" +
                properties.getProperty("api.host") + ":" +
                properties.getProperty("api.port") +
                properties.getProperty("api.path","")
                );

   
            if(printhelp){
                System.out.println(usage);
                String templString = ExpTempl.getExportTempl(properties.getProperty("server.scheme"), properties.getProperty("server.host"), properties.getProperty("server.port"));
                System.out.println(" \n Export template to pass to the plugin registration handler: \n " 
                + templString);
            }
            else {
                System.out.println("\n You can use  -h or --help for more information about usage");   
            }

            if(argexc){
                System.out.println("\n Two much arguments: " + usage);
            }

            jettyServer.join();
        }
        finally {
            jettyServer.stop();
            jettyServer.destroy();
        }
    }

}
