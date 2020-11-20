package de.ids_mannheim.korap.plkexport;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;
import org.junit.Test;

import javax.ws.rs.core.Response;

public class CsvExporterTest {
    
    @Test
    public void testInit () throws IOException {
        CsvExporter csv = new CsvExporter();
        csv.init("{\"query\":\"cool\"}");

        Response resp = csv.serve().build();
        String x = (String) resp.getEntity();
        resp.close();
        assertEquals(x, "HasMoreLeft,leftContext,Match,rightContext,HasMoreRight,"+
                     "isCutted,textSigle,author,pubDate,title\n");
    };

    @Test
    public void testInitFull () throws IOException {
        CsvExporter csv = new CsvExporter();
        csv.init("{\"meta\":\"ja\",\"collection\":\"hm\",\"query\":\"cool\"," +
                 "\"matches\":["+
                 "{\"author\":\"Goethe\","+
                 "\"title\":\"Title1\","+
                 "\"pubDate\":\"20051103\","+
                 "\"textSigle\":\"RTF/G59/34284\","+
                 "\"snippet\":\"Simple <mark>match1</mark> Snippet\"}"+
                 ","+
                 "{\"author\":\"Schiller\","+
                 "\"title\":\"Title2, the\","+
                 "\"pubDate\":\"20051104\","+
                 "\"textSigle\":\"RTF/G59/34285\","+
                 "\"snippet\":\"<span class=\\\"context-left\\\"><span class=\\\"more\\\"></span>"+
                 "Simpler, \\\"faster\\\" </span><span class=\\\"match\\\"><mark>&quot;match2&quot;</mark></span>"+
                 "<span class=\\\"context-right\\\"> Snippet"+
                 "<span class=\\\"more\\\"></span></span>\"}"+
                 "]}");

        Response resp = csv.serve().build();
        String x = (String) resp.getEntity();
        resp.close();
        String[] lines = x.split("\n");
        assertEquals(lines[0],"HasMoreLeft,leftContext,Match,rightContext,HasMoreRight,isCutted,textSigle,author,pubDate,title");
        assertEquals(lines[1],",Simple,match1,Snippet,,,RTF/G59/34284,Goethe,20051103,Title1");
        assertEquals(lines[2],"...,\"Simpler, \"\"faster\"\"\",\"\"\"match2\"\"\",Snippet,...,,RTF/G59/34285,Schiller,20051104,\"Title2, the\"");
        assertEquals(lines.length,3);
    };

    @Test
    public void testAttributes () throws IOException {
        CsvExporter csv = new CsvExporter();
        csv.setFileName("Beispiel");
        assertEquals(csv.getFileName(),"Beispiel");
        assertEquals(csv.getMimeType(),"text/csv");
        assertEquals(csv.getSuffix(),"csv");
    };
};
