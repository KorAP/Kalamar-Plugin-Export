package de.ids_mannheim.korap.plkexport;

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

/**
 * This is a streaming exporter class for Rtf, so it's based on
 * a string buffer.
 */
/*
 * TODO:
 *   - Create a template
 */
public class RtfExporter extends MatchAggregator implements Exporter {

    private static final String HLINE = "{\\pard\\brdrb\\brdrs\\brdrw2\\brsp20\\par}\n";
    
    private boolean firstMatch;

    private ObjectMapper mapper = new ObjectMapper();

    // final static Charset charset = Charset.forName("Windows-1252");
    final static CharsetEncoder charsetEncoder =
        Charset
        .forName("Windows-1252")
        .newEncoder()
        .onMalformedInput(REPORT)
        .onUnmappableCharacter(REPORT);

    StringBuilder sb;

        {
            firstMatch = true;
            sb = new StringBuilder(256);
        }

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
        w.append("{")
            .append("\\rtf1\\ansi\\deff0\n")
            .append("{\\colortbl;\\red0\\green0\\blue0;\\red127\\green127\\blue127;\\red255\\green255\\blue255;}\n")
            .append("{\\fonttbl{\\f0\\fcharset0 Times New Roman;}{\\f1\\fcharset1 Courier;}}\n");

        w.append("{\\footer\\pard\\qr\\fs18\\f0 ");
        rtfText(w, "@ Institut für Deutsche Sprache, Mannheim");

        // Page number
        w.append(" \\endash  \\chpgn /{\\field{\\*\\fldinst{\\fs18\\f0 NUMPAGES}}}");
        w.append("\\par}\n");

        // Title
        if (this.getQueryString() != null) {
            w.append("{\\pard\\fs28\\b\\f1\\ldblquote ");
            rtfText(w, this.getQueryString());
            w.append("\\rdblquote\\par}");
        };

        w.append("\n{\\pard \\par}\n");

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

            MatchExport match = mapper.treeToValue(n, MatchExport.class);

            Snippet s = match.getSnippetO();

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

    private void addInfoTable (Writer w) throws IOException {

        String q = this.getQueryString();

        // Add Information table
        if (q != null && q.length() > 0) {
            this.addInfoRow(w, "Query", this.getQueryString());
        };

        q = this.getCorpusQueryString();
        if (q != null && q.length() > 0) {
            this.addInfoRow(w, "Corpus", q);
        };

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

        if (this.getTotalResults() == -1 ||
            this.getTotalResults() > this.getMaxResults()) {
            this.addInfoRow(w, "Fetched", this.getMaxResults());
        };

        q = this.getSource();
        if (q != null && q.length() > 0) {
            this.addInfoRow(w, "Source", q);
        };

        if (this.getMeta() != null && this.getMeta().has("version")) {
            this.addInfoRow(w, "Backend-Version", this.getMeta().get("version").asText());
        };

        this.addInfoRow(w, "Export-Version", this.getVersion().toString());
    };


    // Add information row
    private void addInfoRow (Writer w, String title, int value) throws IOException {
        this.addInfoRow(w, title, Integer.toString(value));
    };
    

    // Add information row
    private void addInfoRow (Writer w, String title, String value) throws IOException {
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
    
    // Get version for RTF document 
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

    // Based on jrtf by Christian Ullenboom
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
        }
    };
};
