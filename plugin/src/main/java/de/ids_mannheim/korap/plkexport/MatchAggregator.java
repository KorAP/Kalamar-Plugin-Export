package de.ids_mannheim.korap.plkexport;

import java.io.BufferedWriter;
import java.io.File;
import java.io.Writer;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;

import java.util.Collection;
import java.util.ArrayList;

import java.util.Iterator;
import java.util.LinkedList;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

/**
 * Base class for collecting matches and header information
 * for exporters implementing the Exporter interface.
 */

public class MatchAggregator {

    private ObjectMapper mapper = new ObjectMapper();

    private LinkedList<JsonNode> matches;

    private Writer writer;

    private File file;
    
    public JsonNode meta, query, collection;

    public void setMeta (JsonNode meta) {
        this.meta = meta;
    };
   
    public void setQuery (JsonNode query) {
        this.query = query;
    };

    public void setCollection (JsonNode collection) {
        this.collection = collection;
    };

    public void writeHeader (Writer w) throws IOException { };
    public void writeFooter (Writer w) throws IOException { };
    public void addMatch (JsonNode n, Writer w) throws IOException { };
   

    /**
     * Create new match aggregator and parse initial Json
     * file to get header information and initial matches.
     */
    public void init (String resp) throws IOException {

        this.file = null;

        matches = new LinkedList();

        if (resp == null)
            return;
        
        JsonParser parser = mapper.getFactory().createParser(resp);
        JsonNode actualObj = mapper.readTree(parser);

        if (actualObj == null)
            return;

        this.setMeta(actualObj.get("meta"));
        this.setQuery(actualObj.get("query"));
        this.setCollection(actualObj.get("collection"));

        writer = new StringWriter();

        this.writeHeader(writer);
        
        JsonNode mNodes = actualObj.get("matches");

        if (mNodes == null)
            return;
        
        // Iterate over the results of the current file
        Iterator<JsonNode> mNode = mNodes.elements();
        while (mNode.hasNext()) {
            this.addMatch(mNode.next(), writer);
            // this.matches.add(mNode.next());
        };
    };


    /**
     * Append more matches to the result set.
     */
    public void appendMatches (String resp) throws IOException {

        // Open a temp file if not already opened
        if (this.file == null) {

            // Create temporary file
            this.file = File.createTempFile("idsexppl-", ".tmpJson");

            // better delete after it is not needed anymore
            this.file.deleteOnExit();

            String s = writer.toString();

            // Establish writer
            writer = new BufferedWriter(new FileWriter(this.file, true));

            // Add in memory string
            writer.write(s);
        };

        JsonParser parser = mapper.getFactory().createParser(resp);
        JsonNode actualObj = mapper.readTree(parser);

        if (actualObj == null)
            return;
        
        JsonNode mNodes = actualObj.get("matches");

        if (mNodes == null)
            return;

        Iterator<JsonNode> mNode = mNodes.elements();
        
        MatchExport match;
        while (mNode.hasNext()) {
            this.addMatch(mNode.next(), writer);
        };
    };


    /**
     * Serve response entity, either as a string or as a file.
     */
    public ResponseBuilder serve () {
        try {

            this.writeFooter(this.writer);
            this.writer.close();
            
            if (this.file == null) {
                return Response.ok(writer.toString());
            };
            return Response.ok(this.file);            
        }

        // Catch error
        catch (IOException io) {
        };

        // TODO:
        //   Return exporter error
        return Response.status(500).entity("error");
    };
};
