package de.ids_mannheim.korap.plkexport;

import jakarta.ws.rs.core.MediaType;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.io.Writer;


/**
 * Streaming JSON exporter.
 */
public class JsonExporter extends MatchAggregator implements Exporter {

    private boolean firstMatch;

        {
            firstMatch = true;
        }


    @Override
    public String getMimeType () {
        return MediaType.APPLICATION_JSON;
    };


    @Override
    public String getSuffix () {
        return "json";
    };

    
    @Override
    public void writeHeader (Writer w) throws IOException {
        w.append("{");

        boolean header = false;
        
        if (this.getQuery() != null) {
            w.append("\"query\":")
                .append(this.getQuery().toString());
            header = true;
        };

        if (this.getMeta() != null) {
            if (header) {
                w.append(',');
            } else {
                header = true;
            };
            w.append("\"meta\":")
                .append(this.getMeta().toString());
        };

        if (this.getCollection() != null) {
            if (header) {
                w.append(',');
            } else {
                header = true;
            };
            w.append("\"collection\":")
                .append(this.getCollection().toString());
        };

        if (header)
            w.append(',');

        w.append("\"matches\":[");
    };


    @Override
    public void writeFooter (Writer w) throws IOException {
        w.append("]}");
    };
    

    @Override
    public void addMatch (JsonNode n, Writer w) throws IOException {
        if (firstMatch)
            firstMatch = false;
        else
            w.append(',');

        w.append(n.toString());
        return;
    };    
};
