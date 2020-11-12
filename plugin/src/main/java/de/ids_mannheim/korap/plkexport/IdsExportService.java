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

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.tutego.jrtf.*;
import static com.tutego.jrtf.Rtf.rtf;
import static com.tutego.jrtf.RtfPara.*;
import static com.tutego.jrtf.RtfText.*;

import static de.ids_mannheim.korap.plkexport.Util.*;

// Template engine
import freemarker.template.Configuration;
import freemarker.template.Template;

/**
 * TODO:
 * - Paging export works only for rtf, JSON has to be integrated
 * - Delete the temp file of the export at the end
 * - Get variable cutoff from URL
 * - Right now, the web service returns one page (cutoff=1) or
 *   all pages.
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
     * response as json(all of the response) and
     * as rtf(matches)
     * 
     * @param fname
     *            file name
     * @param format
     *            the file format value rtf or json.
     * @param q
     *            the query
     * @param ql
     *            the query language
     * 
     * 
     */
    @POST
    @Path("export")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response testjsonform (
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


        int totalhits;
        
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
            .queryParam("context", "40-t,40-t")
            .queryParam("ql", ql)
            ;

        if (path != "") {
            uri = uri.path(path);
        };

        /*
        if (il != null) {
            uri = uri.queryParam("count", hitc);
        }

        else {
            uri = uri.queryParam("count", ExWSConf.MAX_EXP_LIMIT);
        };
        */

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

            // This is a temporary solution using session riding - only
            // valid for the time being
            Cookie[] cookies = req.getCookies();
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
                            auth = m.group(1);
                            break;
                        };
                    };
                };
                continue;
            };
        };
    
        String resp;
        WebTarget resource;
        try {
            resource = client.target(uri.build());
            Invocation.Builder reqBuilder = resource.request(MediaType.APPLICATION_JSON);
            if (xff != "") {
                reqBuilder = reqBuilder.header("X-Forwarded-For", xff);
            };
            if (auth != "") {
                reqBuilder = reqBuilder.header("Authorization", auth);
            };
            resp = reqBuilder.get(String.class);
        } catch (Exception e) {
            throw new WebApplicationException(
                responseForm(Status.BAD_GATEWAY, "Unable to reach Backend")
                );
        }

        if (fname == null) {
            fname = q;
        }

        // format == json
        if (format.equals("json")) {
            if (cutoff) {
                builder = Response.ok(resp);
                builder.type(MediaType.APPLICATION_JSON);
            };
        }

        // format == rtf
        else {
            ObjectMapper mapper = new ObjectMapper();
            JsonFactory factory = mapper.getFactory();
            JsonParser parser = factory.createParser(resp);
            JsonNode actualObj = mapper.readTree(parser);

            /*
             * Get total results
             */
            totalhits = actualObj.at("/meta").get("totalResults").asInt();
            
            // If only one page should be exported there is no need
            // for a temporary export file
            if (cutoff) {
                String rtfresp = getRtf(resp);
                builder = Response.ok(rtfresp);
            }

            if (!cutoff) {
           
                /*
                 *  Get number of pages and the number of hits 
                 *  which should be exported at the last page
                 */
                int pg = 1;
                int dr = totalhits % pageSize;
                if (dr > 0) {
                    pg = totalhits / pageSize + 1;
                }
                else {
                    pg = totalhits / pageSize;
                }

                /*
                 * Create temporary file
                 */
                File expTmp = createTempFile("idsexppl-", format);
                FileWriter fw = new FileWriter(expTmp, true);
                BufferedWriter bw = new BufferedWriter(fw);
                // better delete after it is not needed anymore
                expTmp.deleteOnExit();
            
                // position of pages, 1 = first page, 2 = middle pages, 3 = last page
                int pos = 0;
                // String urlorg = url;
                uri.queryParam("offset", "{offset}");
                for (int i = 1; i <= pg; i++) {
                    // url = urlorg + "&page=" + i;
                    // resource = client.target(url);
                    resource = client.target(
                        uri.build((i * pageSize) - pageSize)
                        );
                    resp = resource.request(MediaType.APPLICATION_JSON)
                        .get(String.class);

                    if (i < pg) {
                        pos = 2;
                    }
                    if (i == 1) {
                        pos = 1;
                    }
                    if (pg == i) {
                        pos = 3;
                    }
                    getRtf(expTmp, fw, resp, bw, pos, dr);
                }
                builder = Response.ok(expTmp);
            }

            builder.type("application/rtf");
            format = "rtf";
        };        

        builder.header(
            "Content-Disposition",
            "attachment; filename=" +
            sanitizeFileName(fname) +
            '.' +
            format
            );
        Response response = builder.build();
        return response;
    }

    @GET
    @Path("export")
    @Produces(MediaType.TEXT_HTML)
    public Response exportHTML () {
        return responseForm();
    };


    /**
     * Response with form template.
     * 
     * Accepts an optional error code and message.
     */
    private Response responseForm () {
        return responseForm(null, null);
    }

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
        }

        ResponseBuilder resp = Response.ok(out.toString(), "text/html");

        if (code != null)  {
            resp = resp.status(code);
        };

        return resp.build();
    };

    
    @GET
    @Path("export.js")
    @Produces("application/javascript")
    public Response exportJavascript () {
        return Response
            .ok(exportJsStr, "application/javascript")
            .build();
    };


    /*
     * returns export results of one page as rtf String 
     */
    public String getRtf (String resp) throws IOException {
        LinkedList<MatchExport> listMatches = getListMatches(resp);
        return writeRTF(listMatches);
    };


    /* 
     * Writes result of export pages to temporary file
     */
    public void getRtf (File file, FileWriter filewriter, String resp,
            BufferedWriter bw, int pos, int dr) throws IOException {
        LinkedList<MatchExport> listMatches = getListMatches(resp);
        writeRTF(listMatches, file, filewriter, bw, pos, dr);
    };
    
    
    /*
     * returns LinkedList of Matches
     */
    public LinkedList<MatchExport> getListMatches (String resp)
            throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        JsonFactory factory = mapper.getFactory();
        JsonParser parser = factory.createParser(resp);
        JsonNode actualObj = mapper.readTree(parser);
        JsonNode jsonNode1 = actualObj.get("matches");
        LinkedList<MatchExport> listMatches = new LinkedList();
        ObjectMapper objectMapper = new ObjectMapper();
        MatchExport match;

        for (Iterator<JsonNode> itNode = jsonNode1.elements(); itNode
                .hasNext();) {
            match = objectMapper.readValue(itNode.next().toString(),
                    MatchExport.class);
            listMatches.addLast(match);
        };
        return listMatches;
    };
    
  
    /*
     * Iterate over all matches and get an RTF section
     */
    public String getRtfSection (LinkedList list, int pos, int dr) {
        LinkedList matchlist = list;
        RtfTextPara par = p((" "));
        RtfTextPara[] pararray;
        pararray = new RtfTextPara[matchlist.size()];
        Collection<RtfPara> listp = new ArrayList<RtfPara>();

        String reference;
        String textSigle;
        int j = matchlist.size();
        if (dr != 0 && pos == 3) {
            j = dr;
        }
        
        //TODO Add export plugin version to JSON output?
        //
        // TODO 
        // The output rtf file lacks style, 
        // but I'm thinking about changing the jRTF library to OpenRTF https://github.com/LibrePDF/OpenRTF, 
        // because jRTF is very rudimentary, so I only list the information in a section right now.
        //

        RtfTextPara pv = getVersion();
        listp.add(pv);

        for (int i = 0; i < j; i++) {
            MatchExport matchakt = (MatchExport) matchlist.get(i);
            reference = " (" + matchakt.getTitle() + " von "
                + matchakt.getAuthor() + " (" + matchakt.getPubDate() + ")";
            textSigle = "[" + matchakt.getTextSigle() + "]";
            String leftSnippet = matchakt.getSnippetO().getLeft();
            String rightSnippet = matchakt.getSnippetO().getRight();
            String markedMatch = matchakt.getSnippetO().getMark();
            par = p(leftSnippet, (" "), bold(markedMatch), (" "), rightSnippet,
                    bold(reference), (" "), bold(textSigle), "\n");
            listp.add(par);
        }

        String rtfresp = rtf().section(listp).toString();
        return rtfresp;
    };

    
    public String writeRTF (LinkedList list) throws IOException {
        String rtfresp =  getRtfSection(list, 0, 0);
        return rtfresp;
    };
    

    public void writeRTF (LinkedList list, File file, FileWriter filewriter,
                          BufferedWriter bw, int pos, int dr) throws IOException {
   
        String rtfresp = getRtfSection(list, pos,dr);
        
        switch (pos) {

        case 1: {
            rtfresp = rtfresp.substring(0, rtfresp.length() - 1);
            bw.append(rtfresp);
            bw.flush();
            break;
        }

        case 2: {
            rtfresp = rtfresp.substring(143, rtfresp.length() - 1);
            bw.append(rtfresp);
            bw.flush();
            break;
        }

        case 3: {
            rtfresp = rtfresp.substring(143);
            bw.append(rtfresp);
            bw.flush();
            bw.close();
            break;
        }

        default: {
            //TODO Error Handling
            System.out.println("Invalid pos Parameter");
            break;
        }
        };

        return;
    };


    /*
     * Get version for RTF document 
     */
    public RtfTextPara getVersion () {
        Version version = new Version(
            ExWSConf.VERSION_MAJOR,
            ExWSConf.VERSION_MINOR,
            ExWSConf.VERSION_PATCHLEVEL,
            null,
            null,
            null
            );
        RtfTextPara parv = p("@Institut für Deutsche Sprache, Mannheim", ("\n"),
                             "IDSExportPlugin-Version:  ", version, "\n");
        return parv;
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


    /**
     * Creates file to hold the result temporarily
     *
     */
    public static File createTempFile (String name, String suffix) {
        try {
            File temp = File.createTempFile(name, "." + suffix);
            return temp;
            
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    };
};
