package de.ids_mannheim.korap.plkexport;
import com.fasterxml.jackson.databind.JsonNode;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import java.io.IOException;
import java.io.Writer;

interface Exporter {

    // Implemented by MatchAggregator
    public void init (String s) throws IOException;
    public void setMeta(JsonNode n);
    public void setQuery(JsonNode n);
    public void setCollection(JsonNode n);
    public JsonNode getMeta();
    public JsonNode getQuery();
    public JsonNode getCollection();
    public void appendMatches (String s) throws IOException;

    // Implemented by Exporter
    public ResponseBuilder serve();
    
    // Needs to be overwritten
    public void writeHeader (Writer w) throws IOException;
    public void addMatch (JsonNode n, Writer w) throws IOException;
    public void writeFooter (Writer w) throws IOException;
};
