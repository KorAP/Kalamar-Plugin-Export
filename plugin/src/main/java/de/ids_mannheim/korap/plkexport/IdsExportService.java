package de.ids_mannheim.korap.plkexport;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Properties;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tutego.jrtf.*;
import static com.tutego.jrtf.Rtf.rtf;
import static com.tutego.jrtf.RtfPara.*;
import static com.tutego.jrtf.RtfText.*;


@Path("/")
public class IdsExportService {

    Properties properties = ExWSConf.properties(null);
    
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
    public Response testjsonform (@FormParam("fname") String fname,
            @FormParam("format") String format, @FormParam("q") String q,
            @FormParam("ql") String ql, @FormParam("islimit") String il,
            @FormParam("hitc") int hitc) throws IOException {
        

        String[][] params = {
            { "format", format },
            { "q", q },
            { "ql", ql }
        };

        for (int i = 0; i < params.length; i++) {
            if (params[i][1] == null || params[i][1].trim().isEmpty())
                throw new BadRequestException(
                    Response
                    .status(Status.BAD_REQUEST)
                    .entity("Parameter " + "\""
                            + params[i][0] + "\"" + " is missing or empty")
                    .build());
        }

        ResponseBuilder builder;
        Client client = ClientBuilder.newClient();

        String scheme = properties.getProperty("api.scheme", "https");
        String port = properties.getProperty("api.port", "8089");
        String host = properties.getProperty("api.host", "localhost");

        // URIBuildernew UriBuilder();
        UriBuilder uri = UriBuilder.fromPath("/api/v1.0/search")
            .host(host)
            .port(Integer.parseInt(port))
            .scheme(scheme)
            .queryParam("q", q)
            .queryParam("context", "sentence")
            .queryParam("ql", ql)
            .queryParam("cutoff", 1)
            ;
        
        if (il != null) {
            uri = uri.queryParam("count", hitc);
        }

        else {
            uri = uri.queryParam("count", ExWSConf.MAX_EXP_LIMIT);
        }

        //WebTarget resource = client.target(url);
        WebTarget resource = client.target(uri.build());
        String resp = resource.request(MediaType.APPLICATION_JSON)
                .get(String.class);

        if (fname == null) {
            fname = q;
        }

        //format == json
        if (format.equals("json")) {
            builder = Response.ok(resp);
            builder.type(MediaType.APPLICATION_JSON);
            fname = fname + ".json";
        }

        // format == rtf / else
        else {
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
            }

            String rtfresp = writeRTF(listMatches);
            builder = Response.ok(rtfresp);
            fname = fname + ".rtf";
        }

        // TODO:
        //   Sanitize file name (i.e. replace extra characters)
        builder.header("Content-Disposition",
                       "attachment; filename=" + fname);
        Response response = builder.build();
        return response;
    }


    public String writeRTF (LinkedList list) {
        LinkedList matchlist = list;
        RtfTextPara par = p((" "));
        RtfTextPara[] pararray;
        pararray = new RtfTextPara[matchlist.size()];
        Collection<RtfPara> listp = new ArrayList<RtfPara>();

        String reference;
        String textSigle;
        int j = matchlist.size();

        //TODO Add export plugin version to JSON output?
        /*
         * TODO 
         * The output rtf file lacks style, 
         * but I'm thinking about changing the jRTF library to OpenRTF https://github.com/LibrePDF/OpenRTF, 
         * because jRTF is very rudimentary, so I only list the information in a section right now.
         */
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
    }


    public RtfTextPara getVersion () {
        Version version = new Version(ExWSConf.VERSION_MAJOR,
                ExWSConf.VERSION_MINOR, ExWSConf.VERSION_PATCHLEVEL, null, null,
                null);
        RtfTextPara parv = p("@Institut für Deutsche Sprache, Mannheim", ("\n"),
                "IDSExportPlugin-Version:  ", version, "\n");
        return parv;
    }
}
