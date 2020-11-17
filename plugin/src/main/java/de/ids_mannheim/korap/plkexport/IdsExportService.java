package de.ids_mannheim.korap.plkexport;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.lang.Thread;
import java.io.InputStream;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Base64;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;
import javax.servlet.http.Cookie;
import java.net.ConnectException;
import javax.servlet.http.HttpServletRequest;

import static de.ids_mannheim.korap.plkexport.Util.*;

// Template engine
import freemarker.template.Configuration;
import freemarker.template.Template;

/**
 * TODO:
 * - Delete the temp file of the export at the end
 * - Right now, the web service returns one page (cutoff=1) or
 *   all pages.
 * - Handle timeout results (with minimum total results).
 * - Use offset instead of page parameter
 * - Add mime type to exporters
 * - Add format to exporters
 * - Add file suffix to exporters
 * - Test Snippet-Export with multiple classes.
 * - Test Snippet-Export with cutted matches.
 */

@Path("/")
public class IdsExportService {

    Properties properties = ExWSConf.properties(null);

    private final ClassLoader cl = Thread.currentThread().getContextClassLoader();
   
    InputStream is = cl.getResourceAsStream("assets/export.js");
    private final String exportJsStr = streamToString(is);

    Configuration cfg = new Configuration();
    {
        cfg.setClassForTemplateLoading(IdsExportService.class, "/assets/templates");
        cfg.setDefaultEncoding("UTF-8");
    }

    private final static String octets =
        "(?:25[0-5]|2[0-4][0-9]|[0-1]?[0-9]{1,2})";

    private final static String ipre =
        octets + "\\." + octets + "\\." + octets + "\\." + octets;

    private static Pattern authrep = Pattern.compile("\"auth\":\"([^\"]+?)\"");
    
    private final static Base64.Decoder b64Dec = Base64.getDecoder();

    @Context
    private HttpServletRequest req; 
    
    /**
     * WebService calls Kustvakt Search Webservices and returns
     * response as json (all of the response) and
     * as rtf (matches)
     * 
     * @param fname
     *            file name
     * @param format
     *            the file format value rtf or json.
     * @param q
     *            the query
     * @param ql
     *            the query language
     * @param cutoff
     *            Export more than the first page
     * 
     * 
     */
    @POST
    @Path("export")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response export (
        @FormParam("fname") String fname,
        @FormParam("format") String format,
        @FormParam("q") String q,
        @FormParam("ql") String ql,
        @FormParam("islimit") String il,
        @FormParam("cutoff") String cutoffStr,
        @FormParam("hitc") int hitc
        ) throws IOException {

        // These parameters are required
        String[][] params = {
            { "format", format },
            { "q", q },
            { "ql", ql }
        };

        // Check that all parameters are available
        for (int i = 0; i < params.length; i++) {
            if (params[i][1] == null || params[i][1].trim().isEmpty())
                throw new BadRequestException(
                    Response
                    .status(Status.BAD_REQUEST)
                    .entity("Parameter " + "\""
                            + params[i][0] + "\"" + " is missing or empty")
                    .build());
        };


        int totalhits = -1;
        
        // Retrieve cutoff value
        boolean cutoff = false;
        if (cutoffStr != null && (cutoffStr.equals("true") || cutoffStr.equals("1"))) {
            cutoff = true;
        };

        ResponseBuilder builder = null;
        Client client = ClientBuilder.newClient();

        String scheme = properties.getProperty("api.scheme", "https");
        String port   = properties.getProperty("api.port", "8089");
        String host   = properties.getProperty("api.host", "localhost");
        String path   = properties.getProperty("api.path", "");
        int pageSize  = Integer.parseInt(properties.getProperty("conf.page_size", "5"));

        UriBuilder uri = UriBuilder.fromPath("/api/v1.0/search")
            .host(host)
            .port(Integer.parseInt(port))
            .scheme(scheme)
            .queryParam("q", q)
            // .queryParam("context", "sentence")
            .queryParam("context", "40-t,40-t") // Not yet supported
            .queryParam("ql", ql)
            ;

        if (path != "") {
            uri = uri.path(path);
        };

        uri = uri.queryParam("count", pageSize);

        // Get client IP, in case service is behind a proxy
        String xff = "";
        // Get auth (temporarily) via Session riding
        String auth = "";
        if (req != null) {
            xff = getClientIP(req.getHeader("X-Forwarded-For"));
            if (xff == "") {
                xff = req.getRemoteAddr();
            };

            auth = authFromCookie(req);
        };
    
        String resp;
        WebTarget resource;
        Invocation.Builder reqBuilder;
                        
        try {
            resource = client.target(uri.build());
            reqBuilder = resource.request(MediaType.APPLICATION_JSON);
            resp = authBuilder(reqBuilder, xff, auth).get(String.class);
            
        } catch (Exception e) {
            throw new WebApplicationException(
                responseForm(Status.BAD_GATEWAY, "Unable to reach Backend")
                );
        }

        // set filename based on query (if not already set)
        if (fname == null) {
            fname = q;
        }

        Exporter exp;

        if (format.equals("json")) {
            exp = new JsonExporter();
        }
        else {
            exp = new RtfExporter();
        };
        
        exp.init(resp);
        
        // If only one page should be exported there is no need
        // for a temporary export file
        if (cutoff) {

            builder = exp.serve();

            if (format.equals("json")) {
                builder.type(MediaType.APPLICATION_JSON);
            }

            else {
                builder.type("application/rtf");
                format = "rtf";
            };
        }

        // Page through results
        else {

            /*
             * Get total results
             */
            if (exp.getMeta() != null) {
                totalhits = exp.getMeta().get("totalResults").asInt();
            };

            /*
             *  Get number of pages and the number of hits 
             *  which should be exported at the last page
             */
            int pg = 1;
            if (totalhits % pageSize > 0) {
                pg = totalhits / pageSize + 1;
            }
            else {
                pg = totalhits / pageSize;
            }

            uri.queryParam("offset", "{offset}");

            // Iterate over all results
            for (int i = 2; i <= pg; i++) {
                resource = client.target(
                    uri.build((i * pageSize) - pageSize)
                    );

                reqBuilder = resource.request(MediaType.APPLICATION_JSON);
                resp = authBuilder(reqBuilder, xff, auth).get(String.class);
                exp.appendMatches(resp);
            }
            // builder = Response.ok(expTmp);
            builder = exp.serve();

            if (format.equals("json")) {
                builder.type(MediaType.APPLICATION_JSON);
            }
            else {
                builder.type("application/rtf");
                format = "rtf";
            };
        };        

        builder.header(
            "Content-Disposition",
            "attachment; filename=" +
            sanitizeFileName(fname) +
            '.' +
            format
            );
        return builder.build();
    };


    @GET
    @Path("export")
    @Produces(MediaType.TEXT_HTML)
    public Response exportHTML () {
        return responseForm();
    };


    @GET
    @Path("export.js")
    @Produces("application/javascript")
    public Response exportJavascript () {
        return Response
            .ok(exportJsStr, "application/javascript")
            .build();
    };
    

    // Decorate request with auth headers
    private Invocation.Builder authBuilder (Invocation.Builder reqBuilder,
                                            String xff,
                                            String auth) {
        if (xff != "") {
            reqBuilder = reqBuilder.header("X-Forwarded-For", xff);
        };
        if (auth != "") {
            reqBuilder = reqBuilder.header("Authorization", auth);
        };

        return reqBuilder;
    };


    // Get authorization token from cookie
    private String authFromCookie (HttpServletRequest r) {

        // This is a temporary solution using session riding - only
        // valid for the time being
        Cookie[] cookies = r.getCookies();
        String cookiePath = properties.getProperty("cookie.path", "");

        // Iterate through all cookies for a Kalamar session
        for (int i = 0; i < cookies.length; i++) {
                
            // Check the valid path
            if (cookiePath != "" && cookies[i].getPath() != cookiePath) {
                continue;
            };

            // Ignore irrelevant cookies
            if (!cookies[i].getName().matches("^kalamar(-.+?)?$")) {
                continue;
            };

            // Get the value
            String b64 = cookies[i].getValue();
            String[] b64Parts = b64.split("--", 2);
            if (b64Parts.length == 2) {
                // Read the payload
                String payload = new String(b64Dec.decode(b64Parts[0]));
                if (payload != "") {
                    Matcher m = authrep.matcher(payload);
                    if (m.find()) {
                        return m.group(1);
                    };
                };
            };
        };

        return "";
    };

    
    /*
     * Response with form template.
     */
    private Response responseForm () {
        return responseForm(null, null);
    };


    /*
     * Response with form template.
     * 
     * Accepts an error code and message.
     */
    private Response responseForm (Status code, String msg) {
        StringWriter out = new StringWriter();
        HashMap<String, Object> templateData = new HashMap<String, Object>();

        String scheme = properties.getProperty("asset.scheme", "https");
        String port = properties.getProperty("asset.port", "");
        String host = properties.getProperty("asset.host", "korap.ids-mannheim.de");
        String path = properties.getProperty("asset.path", "");

        UriBuilder uri = UriBuilder.fromPath("")
            .host(host)
            .scheme(scheme);

        if (path != "") {
            uri = uri.path(path);
        };

        if (port != "") {
            uri = uri.port(Integer.parseInt(port));
        };

        templateData.put("assetPath", uri.build());

        if (code != null) {
            templateData.put("code", code.getStatusCode());
            templateData.put("msg", msg);            
        };

        // Generate template
        try {
            Template template = cfg.getTemplate("export.ftl");
            template.process(templateData, out);
        }
        catch (Exception e) {
            return Response
                .ok(new String("Template not found"))
                .status(Status.INTERNAL_SERVER_ERROR)
                .build();
        };

        ResponseBuilder resp = Response.ok(out.toString(), "text/html");

        if (code != null)  {
            resp = resp.status(code);
        };

        return resp.build();
    };    


    /*
     * This function is a simplification of
     * Mojolicious::Plugin::ClientIP
     */
    protected static String getClientIP (String xff) {
        if (xff == null) {
            return "";
        };

        String[] ips = xff.split("\\s*,\\s*");

        for (int i = ips.length - 1; i >= 0; i--){
            if (ips[i].matches(ipre)) {
                return ips[i];
            };
        };

        return "";
    };
};
