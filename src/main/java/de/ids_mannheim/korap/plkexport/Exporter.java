package de.ids_mannheim.korap.plkexport;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.ws.rs.core.Response.ResponseBuilder;
import java.io.IOException;
import java.io.Writer;
import org.glassfish.jersey.media.sse.EventOutput;

/**
 * Exporter interface every exporter needs to satisfy.
 */
interface Exporter {

    // Implemented by MatchAggregator
    public boolean init (String s) throws IOException;
    public Exporter finish () throws IOException;
    public void setMeta(JsonNode n);
    public void setQuery(JsonNode n);
    public void setCollection(JsonNode n);
    public JsonNode getMeta();
    public JsonNode getQuery();
    public JsonNode getCollection();
    public boolean appendMatches (String s) throws IOException;
    public String getFileName ();
    public void setFileName (String s);
    public void setFile (String id);
    public String getQueryString ();
    public void setQueryString (String s);
    public String getCorpusQueryString ();
    public void setCorpusQueryString (String s);
    public String getSource ();
    public void setSource (String h, String p);
    public void setSource (String v);
    public int getTotalResults ();
    public boolean hasTimeExceeded ();
    public void setMaxResults (int m);
    public void setSse (EventOutput sse);
    public void forceFile ();
    public String getExportID ();
    public ResponseBuilder serve();
    
    // Needs to be overwritten
    public void writeHeader (Writer w) throws IOException;
    public void addMatch (JsonNode n, Writer w) throws IOException;
    public void writeFooter (Writer w) throws IOException;
    public String getMimeType ();
    public String getSuffix ();
};
