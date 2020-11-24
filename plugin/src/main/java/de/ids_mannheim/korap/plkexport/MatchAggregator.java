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
import java.util.Properties;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;

import org.glassfish.jersey.media.sse.EventOutput;
import org.glassfish.jersey.media.sse.OutboundEvent;

import static de.ids_mannheim.korap.plkexport.Util.*;

/**
 * Base class for collecting matches and header information
 * for exporters implementing the Exporter interface.
 */

public class MatchAggregator {

    private final Properties prop = ExWSConf.properties(null);

    private ObjectMapper mapper = new ObjectMapper();

    private Writer writer;

    private File file;
    
    private JsonNode meta, query, collection;
    private String fname, queryString, corpusQueryString, src;
    private boolean timeExceeded = false;
    private int totalResults = -1;
    private int maxResults = -1;
    private int fetchedResults = 0;
    
    private EventOutput evOut;
    
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

    public void setSource (String host, String path) {
        StringBuilder s = new StringBuilder(32);
        if (host != null)
            s.append(host);

        if (path != null && path.length() > 0)
            s.append('/').append(path);

        this.src = s.toString();
    };

    public String getSource () {
        return this.src;
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

    public String getExportID () {
        if (this.file == null)
            return "";
        return this.file.getName();
    };

    /**
     * Set the file based on the export ID
     */
    public void setFile (String exportID) {
        this.file = new File(
            this.getFileDirectory(),
            exportID
            );
    }
    
    public void writeHeader (Writer w) throws IOException { };
    public void writeFooter (Writer w) throws IOException { };
    public void addMatch (JsonNode n, Writer w) throws IOException { };

    public void setSse (EventOutput eventOutput) {
        this.evOut = eventOutput;
    };


    private File getFileDirectory () {

        String fileDir = prop.getProperty(
            "conf.file_dir",
            System.getProperty("java.io.tmpdir")
            );

        File dir = new File(fileDir);

        // Create directory if not yet existing
        if (!dir.exists()) {
            dir.mkdir();
        }

        else if (!dir.canWrite()) {
            fileDir = System.getProperty("java.io.tmpdir");
            System.err.println("Unable to write to directory");
            System.err.println("Fallback to " + fileDir);
            dir = new File(fileDir);
        };
        return dir;
    };
    
    // Send the progress
    private void sendProgress () {

        if (this.evOut == null || this.maxResults == 0)
            return;

        if (this.evOut.isClosed())
            return;
         
        int calc = (int) Math.ceil(((double) this.fetchedResults / this.maxResults) * 100);

        final OutboundEvent.Builder eventBuilder = new OutboundEvent.Builder();
        eventBuilder.name("Progress");
        eventBuilder.data(String.valueOf(calc));

        try {
            this.evOut.write(eventBuilder.build());
        } catch (IOException e) {
            return;
        };
    };

    /**
     * Force creation of a file, even when only a few
     * matches are requested.
     */
    public void forceFile () {

        // Open file if not already opened
        if (this.file == null) {

            try {

                File dir = getFileDirectory();
            
                // Create temporary file
                this.file = File.createTempFile(
                    "idsexp-", "." + this.getSuffix(),
                    dir
                    );

                // better delete after it is not needed anymore
                // this.file.deleteOnExit();

                String s = null;

                if (writer != null)
                    s = writer.toString();

                // Establish writer
                writer = new BufferedWriter(new FileWriter(this.file, true));

                // Add in memory string
                if (s != null)
                    writer.write(s);

            }
            catch (IOException e) {

                // Will rely on in-memory data
                return;
            };
        };
    };
    

    /**
     * Create new match aggregator and parse initial Json
     * file to get header information and initial matches.
     */
    public boolean init (String resp) throws IOException, JsonParseException {

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

        // In case the writer is already set (e.g. forceFile() was issued),
        // write in the header
        if (writer == null) {
            this.file = null;
            writer = new StringWriter();
        };

        this.writeHeader(writer);

        return this.iterateThroughMatches(
            actualObj.get("matches")
            );
    };


    /**
     * Append more matches to the result set.
     */
    public boolean appendMatches (String resp) throws IOException {

        // Demand creation of a file
        this.forceFile();

        JsonParser parser = mapper.getFactory().createParser(resp);
        JsonNode actualObj = mapper.readTree(parser);

        if (actualObj == null)
            return false;
        
        return this.iterateThroughMatches(
            actualObj.get("matches")
            );
    };

    /**
     * Finalize the export stream.
     */
    public Exporter finish() throws IOException {
        this.writeFooter(this.writer);
        this.writer.close();
        return (Exporter) this;
    };
    

    /**
     * Serve response entity, either as a string or as a file.
     */
    public ResponseBuilder serve () {

        ResponseBuilder rb;

        if (this.file == null) {

            // Serve stream
            rb = Response.ok(writer.toString());
        }
        else if (this.file.exists()) {

            // Serve file
            rb = Response.ok(this.file);
        }
        else {
            // File doesn't exist
            return Response.status(Status.NOT_FOUND);            
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
    };


    // Iterate through all matches
    private boolean iterateThroughMatches (JsonNode mNodes) throws IOException {

        this.sendProgress();

        if (mNodes == null)
            return false;
        
        // Iterate over the results of the current file
        Iterator<JsonNode> mNode = mNodes.elements();
        while (mNode.hasNext()) {
            if (this.maxResults > 0 &&
                this.fetchedResults >= this.maxResults) {
                return false;
            };
            this.addMatch(mNode.next(), writer);
            this.fetchedResults++;
        };
        return true;
    };
};
