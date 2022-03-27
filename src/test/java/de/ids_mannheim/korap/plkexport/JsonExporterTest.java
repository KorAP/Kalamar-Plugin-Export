package de.ids_mannheim.korap.plkexport;

import java.io.IOException;
import java.io.File;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.InputStream;



import jakarta.ws.rs.core.StreamingOutput;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;
import org.junit.Test;

import jakarta.ws.rs.core.Response;

public class JsonExporterTest {

    @Test
    public void testInit () throws IOException {
        JsonExporter json = new JsonExporter();
        json.init("{\"query\":\"cool\"}");
        json.finish();
        Response resp = json.serve().build();
        String x = (String) resp.getEntity();
        resp.close();
        assertEquals(x,"{\"query\":\"cool\",\"matches\":[]}");
    };

    @Test
    public void testInitFull () throws IOException {
        JsonExporter json = new JsonExporter();
        json.init("{\"meta\":\"ja\",\"collection\":\"hm\",\"query\":\"cool\",\"matches\":[\"first\",\"second\"]}");
        json.finish();

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
        json.finish();

        // This is not testable outside the service
        Response resp = json.serve().build();
        StreamingOutput x = (StreamingOutput) resp.getEntity();
        resp.close();        
        assertNotNull(x);        
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
