package de.ids_mannheim.korap.plkexport;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;
import org.junit.Test;

import javax.ws.rs.core.Response;

import de.ids_mannheim.korap.plkexport.JsonExporter;

public class RtfExportTest {

    @Test
    public void testInit () throws IOException {
        RtfExporter rtf = new RtfExporter();
        rtf.init("{\"query\":\"cool\"}");

        Response resp = rtf.serve().build();
        String x = (String) resp.getEntity();
        resp.close();
        assertTrue(x.contains("\\footer\\pard\\qr\\fs18\\f0 @ Institut"));
        assertTrue(x.contains("Institut f\\u252\\'fcr Deutsche"));
    };

    @Test
    public void testInitFull () throws IOException {
        RtfExporter rtf = new RtfExporter();
        rtf.init("{\"meta\":\"ja\",\"collection\":\"hm\",\"query\":\"cool\"," +
                 "\"matches\":["+
                 "{\"author\":\"Goethe\","+
                 "\"title\":\"Title1\","+
                 "\"pubDate\":\"20051103\","+
                 "\"textSigle\":\"RTF/G59/34284\","+
                 "\"snippet\":\"Simple <mark>match1</mark> Snippet\"}"+
                 ","+
                 "{\"author\":\"Schiller\","+
                 "\"title\":\"Title2\","+
                 "\"pubDate\":\"20051104\","+
                 "\"textSigle\":\"RTF/G59/34285\","+
                 "\"snippet\":\"<span class=\\\"context-left\\\"><span class=\\\"more\\\"></span>"+
                 "Simpler </span><span class=\\\"match\\\"><mark>&quot;match2&quot;</mark></span>"+
                 "<span class=\\\"context-right\\\"> Snippet"+
                 "<span class=\\\"more\\\"></span></span>\"}"+
                 "]}");

        Response resp = rtf.serve().build();
        String x = (String) resp.getEntity();
        resp.close();
        assertTrue(x.contains("{\\b match1}"));
        assertTrue(x.contains("{\\b \"match2\"}"));
        assertTrue(x.contains("{\\b Title1"));
        assertTrue(x.contains("{\\b Title2"));

        assertTrue(x.contains("\\qj Simple "));
        assertTrue(x.contains("\\qj [...] Simpler "));
        assertTrue(x.contains("Snippet\\par}"));
        assertTrue(x.contains("Snippet [...]\\par}"));
    };

    @Test
    public void testAttributes () throws IOException {
        RtfExporter rtf = new RtfExporter();
        rtf.setFileName("Beispiel");
        assertEquals(rtf.getFileName(),"Beispiel");
        assertEquals(rtf.getMimeType(),"application/rtf");
        assertEquals(rtf.getSuffix(),"rtf");
    };
};
