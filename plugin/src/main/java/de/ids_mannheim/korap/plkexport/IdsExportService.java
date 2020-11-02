package de.ids_mannheim.korap.plkexport;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;

import javax.ws.rs.BadRequestException;
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

/**
 * TODO
 * This is only a draft!!!
 * Has to be integrated in Nils sourcecode
 * 
 * Export works only for rtf, JSON has to be integrated
 * 
 * Delete the temp file of the export at the end
 * 
 * Get variable cutoff from URL
 * 
 * Right now, the web service returns one page (cutoff=1) or all
 * pages.
 * There is now limitations of hits. ("Beschränken auf xy Treffer")
 * does not work right now.
 * 
 * ------------------------------------------------------------
 * Works with the export demo
 */

@Path("/")
public class IdsExportService {

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


        String[][] params = { { "fname", fname }, { "format", format },
                { "q", q }, { "ql", ql } };

        for (int i = 0; i < params.length; i++) {
            if (params[i][1] == null || params[i][1].trim().isEmpty())
                throw new BadRequestException(Response
                        .status(Status.BAD_REQUEST).entity("Parameter " + "\""
                                + params[i][0] + "\"" + " is missing or empty")
                        .build());
        }


        int totalhits;

        //TODO cutoff to try out, retrieve it  later:
        boolean cutoff = false;
        //boolean cutoff = true;

        ResponseBuilder builder = null;
        Client client = ClientBuilder.newClient();


        String url = "http://localhost:8089/api/v1.0/search?context=sentence"
                + "&q=" + URLEncoder.encode(q, "UTF-8") + "&ql=" + ql;

        url = url + "&cutoff=1" + "&count=" + ExWSConf.PAGE_SIZE;
        WebTarget resource = client.target(url);
        String resp = resource.request(MediaType.APPLICATION_JSON)
                .get(String.class);

        /*
         * Get total results
         */
        ObjectMapper mapper = new ObjectMapper();
        JsonFactory factory = mapper.getFactory();
        JsonParser parser = factory.createParser(resp);
        JsonNode actualObj = mapper.readTree(parser);
        totalhits = actualObj.at("/meta").get("totalResults").asInt();

        //If only one page should be exported there is no need for an temporary export file
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
            int dr = totalhits % ExWSConf.PAGE_SIZE;
            if (dr > 0) {
                pg = totalhits / ExWSConf.PAGE_SIZE + 1;
            }
            else {
                pg = totalhits / ExWSConf.PAGE_SIZE;
            }

            /*
             * Create temporary file
             */
            File expTmp = createTempFile("idsexppl-", format);
            FileWriter fw = new FileWriter(expTmp, true);
            BufferedWriter bw = new BufferedWriter(fw);
            //better delete after it is not needed anymore
            expTmp.deleteOnExit();
            
            // position of pages, 1 = first page, 2 = middle pages, 3 = last page
            int pos = 0;
            String urlorg = url;
            for (int i = 1; i <= pg; i++) {
                url = urlorg + "&page=" + i;
                resource = client.target(url);
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

        builder.header("Content-Disposition",
                "attachment; filename=" + fname + ".rtf");
        Response response = builder.build();
        return response;

    }


    /*
     * returns export results of one page as rtf String 
     */
    public String getRtf (String resp) throws IOException {
        LinkedList<MatchExport> listMatches = getListMatches(resp);
        return writeRTF(listMatches);
    }

    /* 
     * Writes result of export pages to temporary file
     */
    public void getRtf (File file, FileWriter filewriter, String resp,
            BufferedWriter bw, int pos, int dr) throws IOException {
        LinkedList<MatchExport> listMatches = getListMatches(resp);
        writeRTF(listMatches, file, filewriter, bw, pos, dr);
    }

    
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
        }
        return listMatches;
    }
    
  

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

    }

    
    public String writeRTF (LinkedList list) throws IOException {
        String rtfresp =  getRtfSection(list, 0, 0);
        return rtfresp;
    }
    
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
        }

        return;

    }

    /**
     *  Get version for RTF document 
     *  */
    public RtfTextPara getVersion () {
        Version version = new Version(ExWSConf.VERSION_MAJOR,
                ExWSConf.VERSION_MINOR, ExWSConf.VERSION_PATCHLEVEL, null, null,
                null);
        RtfTextPara parv = p("@Institut für Deutsche Sprache, Mannheim", ("\n"),
                "IDSExportPlugin-Version:  ", version, "\n");
        return parv;
    }


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
    }


}
