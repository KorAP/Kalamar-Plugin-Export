package de.ids_mannheim.korap.plkexport;

import java.io.BufferedWriter;
import java.io.File;
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


public class MatchAggregator implements Iterable<JsonNode> {

    private ObjectMapper mapper = new ObjectMapper();

    private LinkedList<JsonNode> matches;

    private BufferedWriter writer;

    public JsonNode meta, query, collection;

    /**
     * Create new match aggregator and parse initial Json
     * file to get header information and initial matches.
     */
    public MatchAggregator (String resp) throws IOException {

        matches = new LinkedList();

        if (resp == null)
            return;
        
        JsonParser parser = mapper.getFactory().createParser(resp);
        JsonNode actualObj = mapper.readTree(parser);

        if (actualObj == null)
            return;

        this.meta       = actualObj.get("meta");
        this.query      = actualObj.get("query");
        this.collection = actualObj.get("collection");


        JsonNode mNodes = actualObj.get("matches");

        if (mNodes == null)
            return;
        
        // Iterate over the results of the current file
        Iterator<JsonNode> mNode = mNodes.elements();
        while (mNode.hasNext()) {
            this.matches.add(mNode.next());
        };
    };


    /**
     * Append more matches to the result set.
     */
    public void append (String resp) throws IOException {

        // Open a temp file if not already opened
        if (writer == null) {

            // Create temporary file
            File expTmp = File.createTempFile("idsexppl-", ".tmpJson");

            // better delete after it is not needed anymore
            expTmp.deleteOnExit();

            // Establish writer
            writer = new BufferedWriter(new FileWriter(expTmp, true));
        };

        JsonParser parser = mapper.getFactory().createParser(resp);
        JsonNode actualObj = mapper.readTree(parser);
        Iterator<JsonNode> mNode = actualObj.get("matches").elements();

        MatchExport match;
        while (mNode.hasNext()) {
            writer.append(mNode.next().toString());
            writer.newLine();
        };
    };


    /**
     * Return an iterator for all matches
     */
    public MatchIterator iterator() { 
        return new MatchIterator(); 
    };


    // Private iterator class
    public class MatchIterator implements Iterator<JsonNode> {
        private int listIndex, fileIndex;

        // Constructor
        public MatchIterator () {
            this.listIndex = matches.size() > 0 ? 0 : -1;

            // Set to zero, if file exists
            this.fileIndex = (writer != null) ? 0 : -1;
        };

        @Override
        public boolean hasNext () {
            if (this.listIndex >= 0 || this.fileIndex >= 0) {
                return true;
            };
            return false;
        };

        @Override
        public JsonNode next () {
            if (this.listIndex >= 0) {
                int i = this.listIndex;
                if (i >= matches.size() - 1) {
                    this.listIndex = -1;
                } else {
                    this.listIndex++;
                };
                return matches.get(i);
            };
            return null;
        };
    };
};
