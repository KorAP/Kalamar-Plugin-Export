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

public class JsonExportTest {

    @Test
    public void testInit () throws IOException {
        JsonExporter json = new JsonExporter();
        json.init("{\"query\":\"cool\"}");

        Response resp = json.serve().build();
        String x = (String) resp.getEntity();
        resp.close();
        assertEquals(x,"{\"query\":\"cool\",\"matches\":[]}");
    };

    @Test
    public void testInitFull () throws IOException {
        JsonExporter json = new JsonExporter();
        json.init("{\"meta\":\"ja\",\"collection\":\"hm\",\"query\":\"cool\",\"matches\":[\"first\",\"second\"]}");

        Response resp = json.serve().build();
        String x = (String) resp.getEntity();
        resp.close();
        assertEquals(x,"{\"query\":\"cool\",\"meta\":\"ja\",\"collection\":\"hm\",\"matches\":[\"first\",\"second\"]}");
    };
};
