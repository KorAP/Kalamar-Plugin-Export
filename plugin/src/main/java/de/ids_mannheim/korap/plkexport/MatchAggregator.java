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
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import static de.ids_mannheim.korap.plkexport.Util.*;

/**
 * Base class for collecting matches and header information
 * for exporters implementing the Exporter interface.
 */

public class MatchAggregator {

    private ObjectMapper mapper = new ObjectMapper();

    private LinkedList<JsonNode> matches;

    private Writer writer;

    private File file;
    
    private JsonNode meta, query, collection;
    private String fname, queryString, corpusQueryString;
    private boolean timeExceeded = false;
    private int totalResults = -1;
    private int maxResults = -1;
    private int fetchedResults = 0;

    public String getMimeType() {
        return "text/plain";
    };

    public String getSuffix() {
        return "txt";
    };

    public int getTotalResults() {
        return this.totalResults;
    };
    
    public boolean hasTimeExceeded() {
        return this.timeExceeded;
    };
    
    public void setFileName (String fname) {
        this.fname = fname;
    };

    public String getFileName () {
        String s = this.fname;
        if (s == null)
            s = this.queryString;
        if (s == null)
            return "export";
        return sanitizeFileName(s);
    };

    public void setQueryString (String query) {
        this.queryString = query;
    };

    public String getQueryString () {
        return this.queryString;
    };

    public void setCorpusQueryString (String query) {
        this.corpusQueryString = query;
    };

    public String getCorpusQueryString () {
        return this.corpusQueryString;
    };
    
    public void setMeta (JsonNode meta) {
        this.meta = meta;
    };

    public JsonNode getMeta () {
        return this.meta;
    };
    
    public void setQuery (JsonNode query) {
        this.query = query;
    };

    // Needs to be set before first addMatch
    public void setMaxResults (int maxResults) {
        this.maxResults = maxResults;
    };

    public int getMaxResults () {
        return this.maxResults;
    };
    
    public JsonNode getQuery () {
        return this.query;
    };

    public void setCollection (JsonNode collection) {
        this.collection = collection;
    };

    public JsonNode getCollection () {
        return this.collection;
    };

    public void writeHeader (Writer w) throws IOException { };
    public void writeFooter (Writer w) throws IOException { };
    public void addMatch (JsonNode n, Writer w) throws IOException { };
   

    /**
     * Create new match aggregator and parse initial Json
     * file to get header information and initial matches.
     */
    public boolean init (String resp) throws IOException, JsonParseException {

        this.file = null;

        matches = new LinkedList();

        if (resp == null)
            return false;

        JsonParser parser = mapper.getFactory().createParser(resp);
        JsonNode actualObj = mapper.readTree(parser);

        if (actualObj == null)
            return false;

        JsonNode meta = actualObj.get("meta");
        this.setMeta(meta);
        this.setQuery(actualObj.get("query"));
        this.setCollection(actualObj.get("collection"));

        if (meta != null) {
            if (meta.has("totalResults")) {
                this.totalResults = meta.get("totalResults").asInt();
                if (meta.has("timeExceeded")) {
                    this.timeExceeded = meta.get("timeExceeded").asBoolean();
                };
            };
        };

        writer = new StringWriter();

        this.writeHeader(writer);

        return this.iterateThroughMatches(
            actualObj.get("matches")
            );
    };


    /**
     * Append more matches to the result set.
     */
    public boolean appendMatches (String resp) throws IOException {

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
            return false;
        
        return this.iterateThroughMatches(
            actualObj.get("matches")
            );
    };


    /**
     * Serve response entity, either as a string or as a file.
     */
    public ResponseBuilder serve () {
        try {
            ResponseBuilder rb;

            this.writeFooter(this.writer);
            this.writer.close();


            if (this.file == null) {
                rb = Response.ok(writer.toString());
            }
            else {
                rb = Response.ok(this.file);
            };

            return rb
                .type(this.getMimeType())
                .header(
                    "Content-Disposition",
                    "attachment; filename=" +
                    this.getFileName() +
                    '.' +
                    this.getSuffix()
                    );
        }

        // Catch error
        catch (IOException io) {
        };

        // TODO:
        //   Return exporter error
        return Response.status(500).entity("error");
    };


    // Iterate through all matches
    private boolean iterateThroughMatches (JsonNode mNodes) throws IOException {
        if (mNodes == null)
            return false;
        
        // Iterate over the results of the current file
        Iterator<JsonNode> mNode = mNodes.elements();
        while (mNode.hasNext()) {
            this.addMatch(mNode.next(), writer);
            this.fetchedResults++;
            if (this.maxResults > 0 &&
                this.fetchedResults > this.maxResults) {
                return false;
            };
        };
        return true;
    };
};
