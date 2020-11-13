package de.ids_mannheim.korap.plkexport;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.io.Writer;

/**
 * This is a streaming exporter class for Json, so it's based on
 * a string buffer.
 */

public class JsonExporter extends MatchAggregator implements Exporter {

    private boolean firstMatch;

        {
            firstMatch = true;
        }
    
    @Override
    public void writeHeader (Writer w) throws IOException {
        w.append("{");

        boolean header = false;
        
        if (this.query != null) {
            w.append("\"query\":")
                .append(this.query.toString());
            header = true;
        };

        if (this.meta != null) {
            if (header) {
                w.append(',');
            } else {
                header = true;
            };
            w.append("\"meta\":")
                .append(this.meta.toString());
        };

        if (this.collection != null) {
            if (header) {
                w.append(',');
            } else {
                header = true;
            };
            w.append("\"collection\":")
                .append(this.collection.toString());
        };

        if (header)
            w.append(',');

        w.append("\"matches\":[");
    }

    @Override
    public void writeFooter (Writer w) throws IOException {
        w.append("]}");
    };
    
    @Override
    public void addMatch (JsonNode n, Writer w) throws IOException {
        if (firstMatch) {
            firstMatch = false;
        }
        else {
            w.append(',');
        };
        w.append(n.toString());
        return;
    };    
};
