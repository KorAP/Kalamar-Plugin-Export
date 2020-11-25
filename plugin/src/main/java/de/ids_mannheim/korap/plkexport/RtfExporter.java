package de.ids_mannheim.korap.plkexport;

import java.util.Properties;

import java.lang.StringBuffer;

import java.nio.charset.*;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import static java.nio.charset.CodingErrorAction.REPORT;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.Version;

import java.io.IOException;
import java.io.Writer;

import de.ids_mannheim.korap.plkexport.Util.*;

/**
 * Streaming RTF exporter.
 */
public class RtfExporter extends MatchAggregator implements Exporter {

    private Properties prop = ExWSConf.properties(null);
    
    // Horizontal line
    private static final String HLINE =
        "{\\pard\\brdrb\\brdrs\\brdrw2\\brsp20\\par}\n";
    
    private static final ObjectMapper mapper = new ObjectMapper();

    final static CharsetEncoder charsetEncoder =
        Charset
        .forName("Windows-1252")
        .newEncoder()
        .onMalformedInput(REPORT)
        .onUnmappableCharacter(REPORT);


    @Override
    public String getMimeType () {
        return "application/rtf";
    };


    @Override
    public String getSuffix () {
        return "rtf";
    };
    
    
    @Override
    public void writeHeader (Writer w) throws IOException {

        String footnote = Util.convertFromUTF8(prop.getProperty("rtf.footnote"));
        
        w.append("{")
            .append("\\rtf1\\ansi\\deff0\n")

            // Color table
            .append("{\\colortbl;\\red0\\green0\\blue0;\\red127\\green127\\blue127;\\red255\\green255\\blue255;}\n")

            // Font table
            .append("{\\fonttbl{\\f0\\fcharset0 Times New Roman;}{\\f1\\fcharset1 Courier;}}\n");

        // Footer on every page, containing the page number
        w.append("{\\footer\\pard\\qr\\fs18\\f0 ");

        if (footnote != null && footnote.length() > 0) {
            rtfText(w, footnote);
            w.append(" \\endash  ");
        };

        w.append("\\chpgn /{\\field{\\*\\fldinst{\\fs18\\f0 NUMPAGES}}}");
        w.append("\\par}\n");

        // Title
        if (this.getQueryString() != null) {
            w.append("{\\pard\\fs28\\b\\f1\\ldblquote ");
            rtfText(w, this.getQueryString());
            w.append("\\rdblquote\\par}");
        };

        w.append("\n{\\pard \\par}\n");

        // Add info table
        this.addInfoTable(w);
    };
    

    @Override
    public void writeFooter (Writer w) throws IOException {
        // Add line
        w.append(HLINE).append("}");
    };
    

    @Override
    public void addMatch (JsonNode n, Writer w) throws IOException {

        try {

            Match match = mapper.treeToValue(n, Match.class);

            Snippet s = match.getSnippet();

            w.append("\\line ");

            // Snippet
            w.append("{\\pard\\fs20\\f0\\qj ");
            if (s.hasMoreLeft()) {
                w.append("[...] ");
            };
            rtfText(w, s.getLeft());
            w.append("{\\b ");
            rtfText(w, s.getMark());
            if (s.isCutted()) {
                w.append(" [!]");
            };
            w.append("}");
            rtfText(w, s.getRight());
            if (s.hasMoreRight()) {
                w.append(" [...]");
            };
            w.append("\\par}");

            // Reference
            w.append("{\\pard");
            w.append("\\qr\\fs18\\cf2\\f0 ");
            w.append("{\\b ");
            rtfText(w, match.getTitle());
            w.append(" von ");
            rtfText(w, match.getAuthor());
            w.append(" (");
            rtfText(w, match.getPubDate());
            w.append(")}");
            w.append("\\par}");

            // TextSigle
            w.append("{\\pard\\qr\\b\\fs18\\cf2\\f1 [");
            rtfText(w, match.getTextSigle());
            w.append("]\\par}");

            w.append("\n");

        } catch (JsonProcessingException jpe) {
            System.err.println(jpe);
            w.append("{\\pard {\\b Unable to process match} \\par}");
        };
    };


    /*
     * Table with meta information about the export.
     */
    private void addInfoTable (Writer w) throws IOException {

        // Query information
        String q = this.getQueryString();
        if (q != null && q.length() > 0) {
            this.addInfoRow(w, "Query", this.getQueryString());
        };

        // Corpus query information
        q = this.getCorpusQueryString();
        if (q != null && q.length() > 0) {
            this.addInfoRow(w, "Corpus", q);
        };

        // Match count information
        if (this.getTotalResults() != -1) {
            StringBuilder str = new StringBuilder(32);
            if (this.hasTimeExceeded()) {
                str.append("> ");
            };
            str.append(Integer.toString(this.getTotalResults()));
            if (this.hasTimeExceeded()) {
                str.append(" (Time exceeded)");
            };

            this.addInfoRow(w, "Count", str.toString());
        };

        // Fetched match count information
        if (this.getTotalResults() == -1 ||
            this.getTotalResults() > this.getMaxResults()) {
            this.addInfoRow(w, "Fetched", this.getMaxResults());
        };

        // Source information
        q = this.getSource();
        if (q != null && q.length() > 0) {
            this.addInfoRow(w, "Source", q);
        };

        // Version information
        if (this.getMeta() != null && this.getMeta().has("version")) {
            this.addInfoRow(w, "Backend-Version", this.getMeta().get("version").asText());
        };

        this.addInfoRow(w, "Export-Version", this.getVersion().toString());
    };


    /*
     * Add information table row
     */
    private void addInfoRow (Writer w, String title, int value) throws IOException {
        this.addInfoRow(w, title, Integer.toString(value));
    };
    

    /*
     * Add information tablerow
     */
    private void addInfoRow (Writer w, String title, String value) throws IOException {

        // Some border and color informations
        w.append("{\\trowd\\trql\\lttrow")
            .append("\\clbrdrt\\brdrs\\clbrdrl\\brdrs\\clbrdrb\\brdrs")
            .append("\\clpadl80\\clpadt80\\clpadb80\\clpadr80\\clcbpat2\\cellx2000")
            .append("\\clbrdrt\\brdrs\\clbrdrl\\brdrs\\clbrdrb\\brdrs\\clbrdrr\\brdrs")
            .append("\\clpadl80\\clpadt80\\clpadb80\\clpadr80\\cellx9300")
            .append("\\intbl\\cf3\\fs18\\b1\\f1 ");
        rtfText(w, title);
        w.append(":\\cell\\cf0\\fs18\\b0\\f1 ");
        rtfText(w, value);
        w.append("\\cell\\row}\n");
    };
    

    /*
     * Get version of the plugin
     * (maybe read from pom?) 
     */
    private Version getVersion () {
        return new Version(
            ExWSConf.VERSION_MAJOR,
            ExWSConf.VERSION_MINOR,
            ExWSConf.VERSION_PATCHLEVEL,
            null,
            null,
            null
            );
    };


    /*
     * Convert a string to RTF compliant encoding.
     *
     * Based on jrtf by Christian Ullenboom
     */
    private static void rtfText(Writer w, String rawText) throws IOException {
        char c;
        for (int i = 0; i < rawText.length(); i++) {
            c = rawText.charAt( i ); 

            if (c == '\n')
                w.append("\\par\n");
            else if (c == '\t' )
                w.append("\\tab\n");
            else if (c == '\\' )
                w.append("\\\\");
            else if (c == '{' )
                w.append("\\{");
            else if (c == '}' )
                w.append("\\}");
            else if (c < 127) {
                w.append(c);
            }

            // Use Unicode and ask the char from the String object
            else {
                w.append("\\u" ).append(Integer.toString(c));

                if (!charsetEncoder.canEncode(c)) {
                    w.append("?");
                };

                try {
                    final ByteBuffer bytes = charsetEncoder.encode(
                        CharBuffer.wrap(String.valueOf(c))
                        );

                    // Treat byte as unsigned
                    final int unsignedCharByte = bytes.get() & 255;
                    w.append(String.format("\\'%02x", unsignedCharByte));
                }
                catch (CharacterCodingException err) {
                };
            };
        };
    };
};
