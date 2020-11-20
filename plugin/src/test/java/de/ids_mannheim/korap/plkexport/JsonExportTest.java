package de.ids_mannheim.korap.plkexport;

import java.io.IOException;
import java.io.File;
import java.io.BufferedReader;
import java.io.FileReader;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;
import org.junit.Test;

import javax.ws.rs.core.Response;

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

    @Test
    public void testPaging () throws IOException {
        JsonExporter json = new JsonExporter();
        json.init("{\"meta\":\"ja\",\"collection\":\"hm\",\"query\":\"cool\",\"matches\":[\"first\",\"second\"]}");
        json.appendMatches("{\"meta\":\"ja2\",\"collection\":\"hm2\",\"query\":\"cool2\",\"matches\":[\"third\",\"fourth\"]}");

        Response resp = json.serve().build();
        File x = (File) resp.getEntity();
        resp.close();
        assertEquals(slurp(x),"{\"query\":\"cool\",\"meta\":\"ja\",\"collection\":\"hm\",\"matches\":[\"first\",\"second\",\"third\",\"fourth\"]}");
    };

    @Test
    public void testAttributes () throws IOException {
        JsonExporter json = new JsonExporter();
        json.setFileName("Beispiel");
        assertEquals(json.getFileName(),"Beispiel");
        assertEquals(json.getMimeType(),"application/json");
        assertEquals(json.getSuffix(),"json");
    };
    

    public static String slurp (File file) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(file)); 	  
        String string; 
	 
        StringBuilder contentBuilder = new StringBuilder();

        while ((string = br.readLine()) != null) 
            contentBuilder.append(string); 

        return contentBuilder.toString();
    };

};
