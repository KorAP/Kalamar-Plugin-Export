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
        RtfExporter json = new RtfExporter();
        json.init("{\"query\":\"cool\"}");

        Response resp = json.serve().build();
        String x = (String) resp.getEntity();
        resp.close();
        assertTrue(x.contains("{\\pard\\ql @ Institut"));
        assertTrue(x.contains("Institut f\\u252\\'fcr Deutsche"));
    };

    @Test
    public void testInitFull () throws IOException {
        RtfExporter json = new RtfExporter();
        json.init("{\"meta\":\"ja\",\"collection\":\"hm\",\"query\":\"cool\"," +
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
                  "\"snippet\":\"Simpler <mark>match2</mark> Snippet\"}"+
                  "]}");

        Response resp = json.serve().build();
        String x = (String) resp.getEntity();
        resp.close();
        assertTrue(x.contains("{\\b match1}"));
        assertTrue(x.contains("{\\b Title1"));
        assertTrue(x.contains("{\\b Title2"));
    };
};
