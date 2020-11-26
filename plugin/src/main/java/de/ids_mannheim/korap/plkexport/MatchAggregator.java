package de.ids_mannheim.korap.plkexport;

import java.io.BufferedWriter;
import java.io.File;
import java.io.Writer;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.FileInputStream;

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
import javax.ws.rs.core.StreamingOutput;

import org.glassfish.jersey.media.sse.EventOutput;
import org.glassfish.jersey.media.sse.OutboundEvent;

import static de.ids_mannheim.korap.plkexport.Util.*;

/**
 * Base class for collecting matches and header information
 * for exporters implementing the Exporter interface.
 */
public class MatchAggregator {

    private final Properties prop = ExWSConf.properties(null);

    private static final ObjectMapper mapper = new ObjectMapper();

    // In-memory and persistant writer for data
    private Writer writer;
    private File file;

    // Meta information for result exports
    private JsonNode meta, query, collection;
    private String fname, queryString, corpusQueryString, src;
    private boolean timeExceeded = false;

    // Result calculations (partially for progress)
    private int totalResults = -1,
        maxResults = -1,
        fetchedResults = 0;

    // Event writer for progress
    private EventOutput evOut;


    /**
     * MimeType of the exporter -
     * defaults to &quot;text/plain&quot; but
     * should be overwritten.
     */
    public String getMimeType() {
        return "text/plain";
    };


    /**
     * Suffix of the exported file -
     * defaults to &quot;txt&quot; but
     * should be overwritten.
     */
    public String getSuffix() {
        return "txt";
    };

    
    /**
     * Total results of exportable matches.
     */
    public int getTotalResults() {
        return this.totalResults;
    };

    
    /**
     * Indicator if time was exceeded when
     * fetching all matches. This means
     * that &quot;totalResults&quot; needs
     * to be treated as a minimum value.
     */
    public boolean hasTimeExceeded() {
        return this.timeExceeded;
    };

    
    /**
     * Set the file name of the file to
     * be exported.
     */
    public void setFileName (String fname) {
        this.fname = fname;
    };

    
    /**
     * Get the file name of the file to
     * be exported.
     */
    public String getFileName () {
        String s = this.fname;
        if (s == null)
            s = this.queryString;
        if (s == null)
            return "export";
        return sanitizeFileName(s);
    };

    
    /**
     * Set the query string.
     */
    public void setQueryString (String query) {
        this.queryString = query;
    };

    
    /**
     * Get the query string.
     */
    public String getQueryString () {
        return this.queryString;
    };

    
    /**
     * Set the corpus query string.
     */
    public void setCorpusQueryString (String query) {
        this.corpusQueryString = query;
    };

    
    /**
     * Get the corpus query string.
     */
    public String getCorpusQueryString () {
        return this.corpusQueryString;
    };

    
    /**
     * Set the source information.
     */
    public void setSource (String host, String path) {
        StringBuilder s = new StringBuilder(32);
        if (host != null)
            s.append(host);

        if (path != null && path.length() > 0)
            s.append('/').append(path);

        this.src = s.toString();
    };

    
    /**
     * Get the source information.
     */
    public String getSource () {
        return this.src;
    };

    
    /**
     * Set the meta JSON blob.
     */
    public void setMeta (JsonNode meta) {
        this.meta = meta;
    };

    
    /**
     * Get the meta JSON blob.
     */
    public JsonNode getMeta () {
        return this.meta;
    };

    
    /**
     * Set the query JSON blob.
     */
    public void setQuery (JsonNode query) {
        this.query = query;
    };

    
    /**
     * Get the query JSON blob.
     */
    public JsonNode getQuery () {
        return this.query;
    };

    
    /**
     * Set the collection JSON blob.
     */
    public void setCollection (JsonNode collection) {
        this.collection = collection;
    };

    
    /**
     * Get the collection JSON blob.
     */
    public JsonNode getCollection () {
        return this.collection;
    };

    
    /**
     * Set the maximum results to be fetched.
     *
     * This needs to be set prior to the first
     * &quot;addMatch&quot; so it can be taken into account.
     */
    public void setMaxResults (int maxResults) {
        this.maxResults = maxResults;
    };

    
    /**
     * Get the maximum results to be fetched.
     */
    public int getMaxResults () {
        return this.maxResults;
    };

    
    /**
     * Get the export ID which is the pointer
     * to where the system can find the temporary
     * generated file.
     */
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

    
    /**
     * Write header for exportation.
     *
     * Should be overwritten.
     */
    public void writeHeader (Writer w) throws IOException { };

    
    /**
     * Write footer for exportation.
     *
     * Should be overwritten.
     */
    public void writeFooter (Writer w) throws IOException { };

    
    /**
     * Write a single match.
     *
     * Should be overwritten.
     */
    public void addMatch (JsonNode n, Writer w) throws IOException { };

    
    /**
     * Set the event stream for progress feedback.
     */
    public void setSse (EventOutput eventOutput) {
        this.evOut = eventOutput;
    };

    
    /**
     * Force the creation of a file, even when only
     * a few matches are requested.
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

                String s = null;

                // Take temporary data from the in-memory writer
                if (writer != null)
                    s = writer.toString();

                // Establish persistant writer
                writer = new BufferedWriter(new FileWriter(this.file, true));

                // Add in-memory string
                if (s != null)
                    writer.write(s);

            }

            // If data can't be stored on disk, the writer will
            // rely on in-memory data, which may or may not work in
            // different contexts.
            catch (IOException e) {
                return;
            };
        };
    };   

    
    /**
     * Parse initial JSON file to get header information
     * and initial matches.
     */
    public boolean init (String resp) throws IOException, JsonParseException {

        if (resp == null)
            return false;

        JsonParser parser = mapper.getFactory().createParser(resp);
        JsonNode root = mapper.readTree(parser);

        if (root == null)
            return false;

        JsonNode meta = root.get("meta");
        this.setMeta(meta);
        this.setQuery(root.get("query"));
        this.setCollection(root.get("collection"));

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

        // Write header to exporter
        this.writeHeader(writer);

        // Go on by iterating through matches
        return this.iterateThroughMatches(root.get("matches"));
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
     * Append more matches to the result set.
     */
    public boolean appendMatches (String resp) throws IOException {

        // Demand creation of a file
        this.forceFile();

        JsonParser parser = mapper.getFactory().createParser(resp);
        JsonNode root = mapper.readTree(parser);

        if (root == null)
            return false;
        
        return this.iterateThroughMatches(root.get("matches"));
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

            // Serve the file and delete after serving
            final File expFile = this.file;
            try {
                final InputStream in = new FileInputStream(this.file);

                // Remove file after output is streamed
                StreamingOutput output = new StreamingOutput() {
                        @Override
                        public void write(OutputStream out)
                            throws IOException {

                            // Write file data in output stream
                            int length;
                            byte[] buffer = new byte[1024];
                            while ((length = in.read(buffer)) != -1) {
                                out.write(buffer, 0, length);
                            }
                            out.flush(); // Important!
                            in.close();

                            // When done, delete the file
                            expFile.delete();
                        }
                    };
            
                // Serve file
                rb = Response.ok(output);
            }

            catch (Exception e) {
                // File problematic
                return Response.status(Status.NOT_FOUND);            
            };
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

    
    /*
     * Iterate through all matches
     */
    private boolean iterateThroughMatches (JsonNode mNodes)
        throws IOException {

        // Send progress information
        this.sendProgress();

        if (mNodes == null)
            return false;
        
        // Iterate over the results of the current file
        Iterator<JsonNode> mNode = mNodes.elements();
        while (mNode.hasNext()) {

            // Stop if all relevant matches are fetched
            if (this.maxResults > 0 &&
                this.fetchedResults >= this.maxResults) {
                return false;
            };
            this.addMatch(mNode.next(), writer);
            this.fetchedResults++;
        };
        return true;
    };

    
    /*
     * Get the directory where all temporary files are stored.
     */
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

        // Directory is unwritable - fallback
        else if (!dir.canWrite()) {
            fileDir = System.getProperty("java.io.tmpdir");
            System.err.println("Unable to write to directory");
            System.err.println("Fallback to " + fileDir);
            dir = new File(fileDir);
        };
        return dir;
    };

    
    /*
     * Send a single progress event to the event stream.
     */
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
        }
        catch (IOException e) {
            return;
        };
    };
};
