package de.ids_mannheim.korap.plkexport;

import java.io.IOException;
import java.io.StringWriter;
import java.io.InputStream;
import java.lang.Thread;
import java.util.HashMap;
import java.util.Properties;
import java.util.Base64;
import java.util.ResourceBundle;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.FormParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.PathParam;
import javax.ws.rs.POST;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.Consumes;
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

import static org.apache.commons.io.FilenameUtils.getExtension;

import org.tinylog.Logger;

import org.glassfish.jersey.media.sse.EventOutput;
import org.glassfish.jersey.media.sse.OutboundEvent;
import org.glassfish.jersey.media.sse.SseFeature;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import org.glassfish.jersey.server.ContainerRequest;

import static de.ids_mannheim.korap.plkexport.Util.*;

// Template engine
import freemarker.template.Configuration;
import freemarker.template.Template;

/**
 * TODO:
 * - Rename "is cutted" to "truncated".
 * - Restructure:
 *   - Load the exporter objects as part of
 *     the MatchAggregator instead of full
 *     inheritance.
 * - Switch Cookie mechanism to container req for
 *   better testing capabilities.
 * - Test ExWsConf.
 * - Do not expect all meta data per match.
 * - Upgrade default pageSize to 50.
 * - Add loading marker.
 * - Add infos to JsonExporter.
 *   - e.g. q &amp; cq string representation.
 * - Check pageSize after init (so pageSize is not
 *   greater than what the server supports).
 * - Restrict CORS to meaningful sources.
 * - Add arbitrary information for RTF header
 * - Add information regarding max_exp_limit
 *   to export form.
 * - Maybe set matches from parent window
 *   (if available) as export default (if
 *   smaller than max_exp_limit)
 * - IDS-internal user should be allowed 100.000
 *   matches per export, while external users
 *   should be limited to 10.000.
 * - Add 1000-separator to numbers.
 * - Get the list of availables locales based
 *   on the given ResourceBundle.
 * - Check for q/ql definition in JS.
 *
 * IDEAS:
 * - Support more granular exporter-specific options
 *   - Create a template mechanism for RTF export.
 *   - Support CSV separator and quote symbol change.
 * - Prettify VC in RTF export (maybe similar to
 *   the visualisation in Kalamar)
 */

@Path("/")
public class Service {

    private Properties prop = ExWSConf.properties(null);

    private final ClassLoader cl = Thread.currentThread().getContextClassLoader();
   
    InputStream is = cl.getResourceAsStream("assets/export.js");
    private final String exportJsStr = streamToString(is);

    Configuration cfg = new Configuration(Configuration.VERSION_2_3_32);

    {
        cfg.setClassForTemplateLoading(Service.class, "/assets/templates");
        cfg.setDefaultEncoding("UTF-8");
    }

    private final static String octets =
        "(?:25[0-5]|2[0-4][0-9]|[0-1]?[0-9]{1,2})";

    private final static String ipre =
        octets + "\\." + octets + "\\." + octets + "\\." + octets;

    private static Pattern authrep = Pattern.compile("\"auth\":\"([^\"]+?)\"");
    
    private final static Base64.Decoder b64Dec = Base64.getDecoder();

    
    @Context
    private HttpServletRequest servletReq;

    @Context
    private ContainerRequest req;

    /*
     * Private method to run the export,
     * either static or streaming
     */
    private Exporter export(String fname,
                             String format,
                             String q,
                             String cq,
                             String ql,
                             String cutoffStr,
                             int hitc,
                             EventOutput eventOutput
        ) throws WebApplicationException {
        
        // These parameters are mandatory
        String[][] params = {
            { "format", format },
            { "q", q },
            { "ql", ql }
        };

        // Check that all mandatory parameters are available
        for (int i = 0; i < params.length; i++) {
            if (params[i][1] == null || params[i][1].trim().isEmpty())
                throw new WebApplicationException(
                    responseForm(Status.BAD_REQUEST,
                                 "Parameter " + "\""
                                 + params[i][0] + "\"" +
                                 " is missing or empty"));
        };
        
        // Retrieve cutoff value
        boolean cutoff = false;
        if (cutoffStr != null && (
                cutoffStr.equals("true") ||
                cutoffStr.equals("1"))
            ) {
            cutoff = true;
        };
        
        // Load configuration values
        String scheme  = prop.getProperty("api.scheme", "https");
        String port    = prop.getProperty("api.port", "8089");
        String host    = prop.getProperty("api.host", "localhost");
        String path    = prop.getProperty("api.path", "");
        String source  = prop.getProperty("api.source");
        int pageSize   = Integer.parseInt(prop.getProperty("conf.page_size", "5"));
        int maxResults = Integer.parseInt(prop.getProperty("conf.max_exp_limit", "10000"));

        // Adjust the number of requested hits
        if (hitc > 0 && hitc < maxResults)
            maxResults = hitc;

        // If less than pageSize results are requested - dont't fetch more
        if (maxResults < pageSize)
            pageSize = maxResults;

        ResponseBuilder builder = null;
        Client client = ClientBuilder.newClient();
        
        // Create initial search uri
        UriBuilder uri = UriBuilder.fromPath("/api/v1.0/search")
            .host(host)
            .port(Integer.parseInt(port))
            .scheme(scheme)
            .queryParam("q", q)
            .queryParam("context", "40-t,40-t")
            .queryParam("ql", ql)
            .queryParam("count", pageSize)
            ;

        // Not yet supported:
        // .queryParam("context", "sentence")
        
        if (cq != null && cq.length() > 0)
            uri = uri.queryParam("cq", cq);
        
        if (path != "")
            uri = uri.path(path);

        // Get client IP, in case service is behind a proxy
        // Get auth (temporarily) via Session riding
        String xff = "", auth = "";
        if (servletReq != null) {
            xff = getClientIP(servletReq.getHeader("X-Forwarded-For"));
            if (xff == "")
                xff = servletReq.getRemoteAddr();

            auth = authFromCookie(servletReq);
        };
    
        String resp;
        WebTarget resource;
        Invocation.Builder reqBuilder;

        try {
            resource = client.target(uri.build());
            reqBuilder = resource.request(MediaType.APPLICATION_JSON);
            resp = authBuilder(reqBuilder, xff, auth).get(String.class);
        }

        catch (Exception e) {
            throw new WebApplicationException(
                responseForm(Status.BAD_GATEWAY, "Unable to reach Backend")
                );
        }

        // Get and initialize exporter based on requested format
        Exporter exp = getExporter(format);
        exp.setMaxResults(maxResults);
        exp.setQueryString(q);
        exp.setCorpusQueryString(cq);
        if (source != null)
            exp.setSource(source);
        else
            exp.setSource(host, path);
       
        // Set filename
        if (fname != null)
            exp.setFileName(fname);

        // Set progress mechanism, if passed
        if (eventOutput != null) {
            exp.setSse(eventOutput);

            // Progress requires the creation
            // of temporary files
            exp.forceFile();
        };

        // Initialize export with meta data
        // and first matches
        try {

            // TODO:
            //   Check return value.
            exp.init(resp);
        }

        catch (Exception e) {
            Logger.error(e);
            String err = e.getMessage();
            if (err == null) {
                err = "Unable to initialize export";
            };

            throw new WebApplicationException(
                responseForm(
                    Status.INTERNAL_SERVER_ERROR,
                    err
                    )
                );
        };

        // Calculate how many results to fetch
        int fetchCount = exp.getTotalResults();
        if (exp.hasTimeExceeded() || fetchCount > maxResults)
            fetchCount = maxResults;

        // fetchCount may be different to maxResults now,
        // so reset after init (for accurate progress)
        exp.setMaxResults(fetchCount);

        // If only one page should be exported there is no need
        // for a temporary export file, unless progress is
        // requested. In case all matches are already fetched,
        // stop here as well.
        if (cutoff || fetchCount <= pageSize) {

            try {

                // Close all export writers
                exp.finish();
            }

            catch (Exception e) {
                Logger.error(e);
                String err = e.getMessage();
                if (err == null) {
                    err = "Unable to finish export";
                };
                
                throw new WebApplicationException(
                    responseForm(
                        Status.INTERNAL_SERVER_ERROR,
                        err
                        )
                    );
            };
            return exp;
        };

        /*
         * Page through all results
         */

        // It's not important anymore to get totalResults
        uri.queryParam("cutoff", "true");

        // Set offset for paging as a template
        uri.queryParam("offset", "{offset}");

        try {
            
            // Iterate over all results
            for (int i = pageSize; i <= fetchCount; i+=pageSize) {

                resource = client.target(uri.build(i));
                reqBuilder = resource.request(MediaType.APPLICATION_JSON);
                resp = authBuilder(reqBuilder, xff, auth).get(String.class);

                // Stop when no more matches are allowed
                if (!exp.appendMatches(resp))
                    break;
            }

            // Close all export writers
            exp.finish();

        }

        catch (Exception e) {
            Logger.error(e);
            String err = e.getMessage();
            if (err == null) {
                err = "Unable to iterate through results";
            };
            throw new WebApplicationException(
                responseForm(
                    Status.INTERNAL_SERVER_ERROR,
                    err
                    )
                );
        };

        return exp;
    };

    
    /**
     * WebService that retrieves data from the Kustvakt
     * Webservice and returns response in different formats.
     *
     * Returns an octet stream.
     * 
     * @param fname
     *            file name
     * @param format
     *            the file format value
     * @param q
     *            the query
     * @param cq
     *            the corpus query
     * @param ql
     *            the query language
     * @param cutoff
     *            Only export the first page
     * @param hitc
     *            Number of matches to fetch
     */
    @POST
    @Path("export")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response staticExport (
        @FormParam("fname") String fname,
        @FormParam("format") String format,
        @FormParam("q") String q,
        @FormParam("cq") String cq,
        @FormParam("ql") String ql,
        @FormParam("cutoff") String cutoffStr,
        @FormParam("hitc") int hitc
        ) throws IOException {

        Exporter exp = export(fname, format, q, cq, ql, cutoffStr, hitc, null);
        
        return exp.serve().build();
    };


    /**
     * WebService that retrieves data from the Kustvakt
     * Webservice and returns response in different formats.
     *
     * Returns an event stream.
     * 
     * @param fname
     *            file name
     * @param format
     *            the file format value
     * @param q
     *            the query
     * @param cq
     *            the corpus query
     * @param ql
     *            the query language
     * @param cutoff
     *            Only export the first page
     * @param hitc
     *            Number of matches to fetch
     */
    @GET
    @Path("export")
    @Produces(SseFeature.SERVER_SENT_EVENTS)
    @Consumes(SseFeature.SERVER_SENT_EVENTS)
	public Response progressExport(
        @QueryParam("fname") String fname,
        @QueryParam("format") String format,
        @QueryParam("q") String q,
        @QueryParam("cq") String cq,
        @QueryParam("ql") String ql,
        @QueryParam("cutoff") String cutoffStr,
        @QueryParam("hitc") int hitc
        ) throws InterruptedException {

        // See
        //   https://www.baeldung.com/java-ee-jax-rs-sse
        //   https://www.howopensource.com/2016/01/java-sse-chat-example/
        //   https://csetutorials.com/jersey-sse-tutorial.html
        //   https://eclipse-ee4j.github.io/jersey.github.io/documentation/latest/sse.html
        
        final EventOutput eventOutput = new EventOutput();

        // Send initial event
        if (eventOutput.isClosed())
            return Response.ok("EventSource closed").build();

        new Thread(
            new Runnable() {

                @Override
                public void run() {
                    final OutboundEvent.Builder eventBuilder = new OutboundEvent.Builder();
                    try {
                        eventBuilder.name("Process");
                        eventBuilder.data("init");
                        eventOutput.write(eventBuilder.build());
                        Exporter exp = export(
                            fname, format, q, cq, ql, cutoffStr, hitc, eventOutput
                            );

                        if (eventOutput.isClosed())
                            return;
                        
                        eventBuilder.name("Relocate");
                        eventBuilder.data(exp.getExportID() + ";" + exp.getFileName());
                        eventOutput.write(eventBuilder.build());
                        
                    }

                    catch (Exception e) {
                        try {
                            if (eventOutput.isClosed())
                                return;

                            eventBuilder.name("Error");
                            eventBuilder.data(e.getMessage());
                            eventOutput.write(eventBuilder.build());
                        }

                        catch (IOException ioe) {
                            Logger.error(ioe);
                            throw new RuntimeException(
                                "Error when writing event output.", ioe
                                );
                        };
                    }

                    finally {
                        try {
                            if (eventOutput.isClosed())
                                return;

                            eventBuilder.name("Process");
                            eventBuilder.data("done");
                            eventOutput.write(eventBuilder.build());                        
                            eventOutput.close();
                        }

                        catch (IOException ioClose) {
                            Logger.error(ioClose);
                            throw new RuntimeException(
                                "Error when closing the event output.", ioClose
                                );
                        }
                    };
                    return;
                }
            }).start();      

        String origin = prop.getProperty("server.origin","*");
        if (servletReq != null) {
            // This is temporary to allow for session riding
            origin = servletReq.getHeader("Origin");
        };

        return Response.ok(eventOutput, SseFeature.SERVER_SENT_EVENTS_TYPE)
            .header("Access-Control-Allow-Origin", origin)
            .header("Access-Control-Allow-Credentials", "true")
            .header("Vary","Origin")
            .build();
    };


    /**
     * Relocation target to which the event
     * stream points to.
     *
     * Returns an octet stream.
     * 
     * @param fname
     *            file name
     * @param file
     *            the file to fetch
     */
    @GET
    @Path("export/{file}")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
	public Response fileExport(
        @PathParam("file") String fileStr,
        @QueryParam("fname") String fname
        ) {

        String format = getExtension(fileStr);
        
        // Get exporter object
        Exporter exp = getExporter(format);

        if (fname != null)
            exp.setFileName(fname);

        exp.setFile(fileStr);

        // Return without init
        return exp.serve().build();
    };
    
    
    /**
     * The export form.
     *
     * Returns a HTML file.
     */
    @GET
    @Path("export")
    @Produces(MediaType.TEXT_HTML)
    public Response exportHTML () {
        return responseForm();
    };


    /**
     * The export script.
     *
     * Returns a static JavaScript file.
     */
    @GET
    @Path("export.js")
    @Produces("application/javascript")
    public Response exportJavascript () {
        return Response
            .ok(exportJsStr, "application/javascript")
            .build();
    };


    /*
     * Get exporter object by format
     */
    private Exporter getExporter (String format) {
        // Choose the correct exporter
        if (format.equals("json"))
            return new JsonExporter();
        else if (format.equals("csv"))
            return new CsvExporter();
        
        return new RtfExporter();
    };
   

    /*
     * Decorate request with auth headers
     */
    private Invocation.Builder authBuilder (
        Invocation.Builder reqBuilder,
        String xff,
        String auth
        ) {

        if (xff != "")
            reqBuilder = reqBuilder.header("X-Forwarded-For", xff);

        if (auth != "")
            reqBuilder = reqBuilder.header("Authorization", auth);

        return reqBuilder;
    };


    /*
     * Get authorization token from cookie
     */
    private String authFromCookie (HttpServletRequest r) {

        // This is a temporary solution using session riding - only
        // valid for the time being
        Cookie[] cookies = r.getCookies();

        if (cookies == null)
            return "";
        
        String cookieName = prop.getProperty("cookie.name", "");

        // Iterate through all cookies for a Kalamar session
        for (int i = 0; i < cookies.length; i++) {

            // Check the valid name and ignore irrelevant cookies
            if (cookieName == "" && !cookies[i].getName().equals("kalamar"))
                continue;

            if (!cookies[i].getName().equals(cookieName))
                continue;

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

        // Build uri for assets
        String scheme = prop.getProperty("asset.scheme", "https");
        String port = prop.getProperty("asset.port", "");
        String host = prop.getProperty("asset.host", "korap.ids-mannheim.de");
        String path = prop.getProperty("asset.path", "");
        String defaultHitc = prop.getProperty("conf.default_hitc", "100");
        int maxHitc = Integer.parseInt(prop.getProperty("conf.max_exp_limit", "10000"));

        UriBuilder uri = UriBuilder.fromPath("")
            .host(host)
            .scheme(scheme);

        if (path != "")
            uri = uri.path(path);

        if (port != "")
            uri = uri.port(Integer.parseInt(port));

        templateData.put("assetPath", uri.build());
        templateData.put("defaultHitc", defaultHitc);
        templateData.put("maxHitc", maxHitc);

        // There is an error code to pass
        if (code != null) {
            templateData.put("code", code.getStatusCode());
            templateData.put("msg", msg);            
        };

        try {
            templateData.put("dict", this.getDictionary());

        } catch (Exception e) {
            Logger.error(e);
            return Response
                .ok(new String("Dictionary not found"))
                .status(Status.INTERNAL_SERVER_ERROR)
                .build();
        };

        // Generate template
        try {
            Template template = cfg.getTemplate("export.ftl");
            template.setLocale(getPreferredSupportedLocale());
            template.process(templateData, out);
        }

        // Unable to find template
        catch (Exception e) {
            Logger.error(e);
            return Response
                .ok(new String("Template not found"))
                .status(Status.INTERNAL_SERVER_ERROR)
                .build();
        };

        ResponseBuilder resp = Response.ok(out.toString(), "text/html");

        if (code != null)
            resp = resp.status(code);

        return resp.build();
    };    


    /*
     * Get the origin user IP.
     *
     * This function is a simplification of
     * Mojolicious::Plugin::ClientIP
     */
    protected static String getClientIP (String xff) {
        if (xff == null) {
            return "";
        };

        String[] ips = xff.split("\\s*,\\s*");

        for (int i = ips.length - 1; i >= 0; i--) {
            if (ips[i].matches(ipre))
                return ips[i];
        };

        return "";
    };


    /*
     * Load dictionary for a chosen locale as a resource bundle
     */
    private ResourceBundle getDictionary() throws IOException {
        return ResourceBundle.getBundle(
                "locales/export", getPreferredSupportedLocale()
        );
    }

    private Locale getPreferredSupportedLocale() throws IOException {
        Locale fallback = new Locale("en");

        if (req != null) {
            for (Locale l : req.getAcceptableLanguages()) {
                switch (l.getLanguage()) {
                    case "de":
                        return (l);
                    case "en":
                        return (l);
                }
            }
        }

        return fallback;
    }

}
