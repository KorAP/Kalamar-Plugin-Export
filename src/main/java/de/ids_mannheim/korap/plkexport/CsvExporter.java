package de.ids_mannheim.korap.plkexport;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.io.Writer;

/**
 * Streaming CSV exporter.
 */
public class CsvExporter extends MatchAggregator implements Exporter {

    private static final ObjectMapper mapper = new ObjectMapper();

    @Override
    public String getMimeType () {
        return "text/csv";
    };


    @Override
    public String getSuffix () {
        return "csv";
    };
   

    @Override
    public void writeHeader (Writer w) throws IOException {
        this.addRecord(
            w,
            new String[]{
                "HasMoreLeft",
                "leftContext",
                "Match",
                "rightContext",
                "HasMoreRight",
                "isCutted",
                "textSigle",
                "author",
                "pubDate",
                "title"
            }
            );
    };
    

    @Override
    public void addMatch (JsonNode n, Writer w) throws IOException {
        Match m = mapper.treeToValue(n, Match.class);
        Snippet s = m.getSnippet();

        String left = s.getLeft(),
            mark = s.getMark(),
            right = s.getRight();

        // For CSV export the Snippet
        // fragments are trimmed
        if (left != null)
            left = left.trim();

        if (mark != null)
            mark = mark.trim();

        if (right != null)
            right = right.trim();

        this.addRecord(
            w,
            new String[]{
                s.hasMoreLeft() ? "..." : "",
                left,
                mark,
                right,
                s.hasMoreRight() ? "..." : "",
                s.isCutted() ? "!" : "",
                m.getTextSigle(),
                m.getAuthor(),
                m.getPubDate(),
                m.getTitle()
            });
    };


    /*
     * Add a CSV row to the CSV stream
     */
    private void addRecord (Writer w, String[] ss) throws IOException {
        this.addCell(w , ss[0]);
        for (int i = 1; i < 10; i++) {
            w.append(',');
            this.addCell(w , ss[i]);
        };
        w.append("\n");
    };
    

    /*
     * Add a CSV cell to the CSV row
     */
    private void addCell (Writer w, String s) throws IOException {

        // If meta characters exist, make a quote
        if (s.contains(",")  ||
            s.contains("\"") ||
            s.contains("\n") ||
            s.contains(" ")  ||
            s.contains("\t") ||
            s.contains(";")) {

            // Iterate over all characters
            // and turn '"' into '""'.
            w.append('"');
            for (int i = 0; i < s.length(); i++) {
                final char c = s.charAt(i);
                if (c == '"') {
                    w.append('"').append('"');
                }
                else {
                    w.append(c);
                };
            };
            w.append('"');
        }

        // No escaping required
        else {
            w.append(s);
        };
    };
};
