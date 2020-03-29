package de.ids_mannheim.korap.plkexport;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tutego.jrtf.*;
import static com.tutego.jrtf.Rtf.rtf;
import static com.tutego.jrtf.RtfPara.*;
import static com.tutego.jrtf.RtfText.*;

@Path("/")
public class IdsExportService {

    /**
     * WebService calls Kustvakt Search Webservices and returns
     * response as json(all of the response) and
     * as rtf(matches)
     * 
     */
    @POST
    @Path("export")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response testjsonform (@FormParam("fname") String fname,
            @FormParam("format") String format, @FormParam("q") String q,
            @FormParam("ql") String ql) throws IOException {


        Client client = ClientBuilder.newClient();

        String url = "http://localhost:8089/api/v1.0/search?context=sentence"
                + "&q=" + URLEncoder.encode(q, "UTF-8") + "&ql=" + ql;
        WebTarget resource = client.target(url);
        String resp = resource.request(MediaType.APPLICATION_JSON)
                .get(String.class);
        ResponseBuilder builder;

        //format == json
        if (format.equals("json")) {
            builder = Response.ok(resp);
            builder.header("Content-Disposition",
                    "attachment; filename=" + fname + ".json");
            builder.type(MediaType.APPLICATION_JSON);
            Response response = builder.build();
            return response;
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
                listMatches.addFirst(match);
            }


            String rtfresp = writeRTF(listMatches);
            builder = Response.ok(rtfresp);
            builder.header("Content-Disposition",
                    "attachment; filename=" + fname + ".rtf");
            Response response = builder.build();
            return response;
        }
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
}
