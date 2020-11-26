package de.ids_mannheim.korap.plkexport;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;

import java.util.Properties;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

public class AssetTest extends JerseyTest {
    
    @Override
    protected Application configure () {
        return new ResourceConfig(Service.class);
    }

    @Test
    public void testFormHtml () {
        Response responsehtml = target("/export").request()
                .get();
        assertEquals("HTTP Code",
                Status.OK.getStatusCode(), responsehtml.getStatus());
        String str = responsehtml.readEntity(String.class);
        assertTrue("HTTP Body", str.contains("<title>Export</title>"));
        assertTrue("Assets", str.contains("<script src=\"https://korap.ids-mannheim.de/js"));
        assertTrue("Assets", str.contains("<link href=\"https://korap.ids-mannheim.de/css"));
        assertFalse("Errors", str.contains("dynCall("));
    }

    @Test
    public void testFormHtmlLocalization () {

        // Check german
        Response responsehtml = target("/export").request()
            .header("Accept-Language","fr-CH, fr;q=0.9, de;q=0.8, en;q=0.7, *;q=0.5").get();
        assertEquals("HTTP Code",
                     Status.OK.getStatusCode(), responsehtml.getStatus());
        String str = responsehtml.readEntity(String.class);
        assertTrue("HTTP Body (de)", str.contains("Dateiformat"));

        // Check English
        responsehtml = target("/export").request()
            .header("Accept-Language","fr-CH, fr;q=0.9, en;q=0.8, de;q=0.7, *;q=0.5").get();
        assertEquals("HTTP Code",
                     Status.OK.getStatusCode(), responsehtml.getStatus());
        str = responsehtml.readEntity(String.class);
        assertTrue("HTTP Body (en)", str.contains("File format"));

        // Check German (2)
        responsehtml = target("/export").request()
            .header("Accept-Language","de-DE, fr;q=0.9, en;q=0.8, de;q=0.7, *;q=0.5").get();
        assertEquals("HTTP Code",
                     Status.OK.getStatusCode(), responsehtml.getStatus());
        str = responsehtml.readEntity(String.class);
        assertTrue("HTTP Body (de)", str.contains("Dateiformat"));
    };

    
    @Test
    public void testFormHtmlAssets () {
        Properties properties = ExWSConf.properties(null);
        String hostTemp = properties.getProperty("asset.host");
        String pathTemp = properties.getProperty("asset.path");
        properties.setProperty("asset.host", "ids-mannheim.example");
        properties.setProperty("asset.path", "/instance/test");
        
        Response responsehtml = target("/export").request()
                .get();
        assertEquals("HTTP Code",
                Status.OK.getStatusCode(), responsehtml.getStatus());
        String str = responsehtml.readEntity(String.class);
        assertTrue("HTTP Body", str.contains("<title>Export</title>"));
        assertTrue("Assets", str.contains("<script src=\"https://ids-mannheim.example/instance/test/js"));
        assertTrue("Assets", str.contains("<link href=\"https://ids-mannheim.example/instance/test/css"));
        assertFalse("Errors", str.contains("dynCall("));

        properties.setProperty("asset.host", hostTemp);
        properties.setProperty("asset.path", pathTemp != null ? pathTemp : "");
    }

    @Test
    public void testFormHtmlExporters () {
        Response responsehtml = target("/export").request()
                .get();
        assertEquals("HTTP Code",
                Status.OK.getStatusCode(), responsehtml.getStatus());
        String str = responsehtml.readEntity(String.class);
        assertTrue("HTTP Body", str.contains("<title>Export</title>"));
        assertTrue("RTF", str.contains("id=\"formatrtf\""));
        assertTrue("RTF-Label", str.contains("for=\"formatrtf\""));
        assertTrue("JSON", str.contains("id=\"formatjson\""));
        assertTrue("JSON-Label", str.contains("for=\"formatjson\""));
        assertTrue("CSV", str.contains("id=\"formatcsv\""));
        assertTrue("CSV-Label", str.contains("for=\"formatcsv\""));
        assertFalse("DOC", str.contains("id=\"formatdoc\""));
    }

    
    @Test
    public void testJS () {
        Response responsejs = target("/export.js").request()
                .get();
        assertEquals("HTTP Code",
                Status.OK.getStatusCode(), responsejs.getStatus());
        String str = responsejs.readEntity(String.class);

        assertTrue("HTTP Body", str.contains("pluginit"));
    }
};
