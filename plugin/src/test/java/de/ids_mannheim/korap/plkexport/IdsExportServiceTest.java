package de.ids_mannheim.korap.plkexport;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

// Mockserver tests
import org.mockserver.integration.ClientAndServer;
import org.mockserver.client.server.MockServerClient;
import org.mockserver.junit.MockServerRule;
import static org.mockserver.model.HttpRequest.*;
import static org.mockserver.model.HttpResponse.*;
import org.junit.BeforeClass;
import org.junit.AfterClass;
import org.slf4j.Logger;

// Fixture loading
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URLDecoder;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.junit.Test;

import de.ids_mannheim.korap.plkexport.IdsExportService;
import de.ids_mannheim.korap.plkexport.ExWSConf;

/*
 * TODO Find a way to test efficiently the starting of the PluginServer with host and port of the config-file
 * + serving static files
 */

public class IdsExportServiceTest extends JerseyTest {

    private static ClientAndServer mockServer;
	private static MockServerClient mockClient;
    
    @BeforeClass
    public static void startServer() {
        // Define logging rules for Mock-Server
        ((ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory
         .getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME))
            .setLevel(ch.qos.logback.classic.Level.OFF);
        ((ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory
         .getLogger("org.mockserver"))
            .setLevel(ch.qos.logback.classic.Level.OFF);

        mockServer = ClientAndServer.startClientAndServer(34765);
        mockClient = new MockServerClient("localhost", mockServer.getPort());

        Properties properties = ExWSConf.properties(null);
        properties.setProperty("api.host", "localhost");
        properties.setProperty("api.port", String.valueOf(mockServer.getPort()));
        properties.setProperty("api.scheme", "http");
    }
   
    @AfterClass
    public static void stopServer() {
        mockServer.stop();
    }
    
    @Override
    protected Application configure () {
        return new ResourceConfig(IdsExportService.class);
    }

    // Client is pre-configured in JerseyTest
    /**
     * Tests if webservice returns a document with the right filename
     * and file format and handles empty/missing parameters correctly.
     */
    @Test
    public void testExportWsJson () {
        mockClient.reset().when(
            request()
            .withMethod("GET")
            .withPath("/api/v1.0/search")
            )
            .respond(
                response()
                .withHeader("Content-Type: application/json; charset=utf-8")
                .withBody(getFixture("response_water.json"))
                .withStatusCode(200)
                );

        String filenamej = "dateiJson";
        MultivaluedHashMap<String, String> frmap = new MultivaluedHashMap<String, String>();
        frmap.add("fname", filenamej);
        frmap.add("format", "json");
        frmap.add("q", "Wasser");
        frmap.add("ql", "poliqarp");

        String message;

        Response responsejson = target("/export").request()
                .post(Entity.form(frmap));
        
        assertEquals("Request JSON: Http Response should be 200: ",
                Status.OK.getStatusCode(), responsejson.getStatus());
        // A JSON document should be returend
        assertEquals("Request JSON: Http Content-Type should be: ",
                MediaType.APPLICATION_JSON,
                responsejson.getHeaderString(HttpHeaders.CONTENT_TYPE));
        // Results should not be displayed inline but saved and displayed locally
        assertTrue(
                "Request JSON: Results should not be displayed inline, but saved and displayed locally",
                responsejson.getHeaderString(HttpHeaders.CONTENT_DISPOSITION)
                        .contains("attachment"));
        // The document should be named correctly
        assertTrue("Request JSON: Filename should be set correctly: ",
                   responsejson.getHeaderString(HttpHeaders.CONTENT_DISPOSITION)
                   .contains("filename=" + filenamej));

        frmap.remove("ql");
        responsejson = target("/export").request()
                .post(Entity.form(frmap));
        assertEquals("Request JSON: Http Response should be 400: ",
                Status.BAD_REQUEST.getStatusCode(), responsejson.getStatus());

        frmap.remove("q");
        responsejson = target("/export").request()
                .post(Entity.form(frmap));
        assertEquals("Request JSON: Http Response should be 400: ",
                Status.BAD_REQUEST.getStatusCode(), responsejson.getStatus());

    }

    @Test
    public void testFormHTML () {
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
    public void testJS () {
        Response responsejs = target("/export.js").request()
                .get();
        assertEquals("HTTP Code",
                Status.OK.getStatusCode(), responsejs.getStatus());
        String str = responsejs.readEntity(String.class);

        assertTrue("HTTP Body", str.contains("pluginit"));
    }
    
    @Test
    public void testExportWsJsonEmpty () {
        mockClient.reset().when(
            request()
            .withMethod("GET")
            .withPath("/api/v1.0/search")
            .withQueryStringParameter("q", "????")
            )
            .respond(
                response()
                .withHeader("Content-Type: application/json; charset=utf-8")
                .withBody(getFixture("response_none.json"))
                .withStatusCode(200)
                );

        String filenamej = "dateiJson";
        MultivaluedHashMap<String, String> frmap = new MultivaluedHashMap<String, String>();
        frmap.add("fname", filenamej);
        frmap.add("format", "json");
        frmap.add("q", "????");
        frmap.add("ql", "poliqarp");

        String message;

        Response responsejson = target("/export").request()
                .post(Entity.form(frmap));
        
        assertEquals("Request JSON: Http Response should be 200: ",
                Status.OK.getStatusCode(), responsejson.getStatus());
    };
    
    
    @Test
    public void testExportWsRTF () {
        mockClient.reset().when(
            request()
            .withMethod("GET")
            .withPath("/api/v1.0/search")
            .withQueryStringParameter("q", "Wasser")
            )
            .respond(
                response()
                .withHeader("Content-Type: application/json; charset=utf-8")
                .withBody(getFixture("response_water.json"))
                .withStatusCode(200)
                );

        MultivaluedHashMap<String, String> frmap = new MultivaluedHashMap<String, String>();
        frmap.add("format", "rtf");
        frmap.add("q", "Wasser");
        frmap.add("ql", "poliqarp");
        String filenamer = "dateiRtf";
        frmap.putSingle("fname", filenamer);

        String message;

        Response responsertf = target("/export").request()
            .post(Entity.form(frmap));
        assertEquals("Request RTF: Http Response should be 200: ",
                Status.OK.getStatusCode(), responsertf.getStatus());
        // An RTF document should be returned
        assertEquals("Request RTF: Http Content-Type should be: ",
                MediaType.APPLICATION_OCTET_STREAM,
                responsertf.getHeaderString(HttpHeaders.CONTENT_TYPE));
        // Results should not be displayed inline but saved and displayed locally
        assertTrue("Request RTF: Results should not be displayed inline",
                responsertf.getHeaderString(HttpHeaders.CONTENT_DISPOSITION)
                        .contains("attachment"));
        // The document should be named correctly
        assertTrue("Request RTF: Filename should be set correctly: ",
                responsertf.getHeaderString(HttpHeaders.CONTENT_DISPOSITION)
                        .contains("filename=" + filenamer));

        Response resp;
        String fvalue;
        frmap.remove("fname");
        MultivaluedHashMap<String, String> map = new MultivaluedHashMap<String, String>();
        map.putAll(frmap);

        //Checks missing or empty parameters
        for (String fkey : frmap.keySet()) {
            //parameter is missing
            fvalue = frmap.getFirst(fkey);
            map.remove(fkey);
            resp = target("/export").request().post(Entity.form(map));
            assertEquals("Request RTF: Http Response should be 400: ",
                    Status.BAD_REQUEST.getStatusCode(), resp.getStatus());
            message = resp.readEntity(String.class);
            assertTrue(
                    "Right Exception Message should be returned for missing format",
                    message.contains(
                            "Parameter \"" + fkey + "\" is missing or empty"));
            //parameter is empty
            map.putSingle(fkey, "");
            resp = target("/export").request().post(Entity.form(map));
            assertEquals("Request RTF: Http Response should be 400: ",
                    Status.BAD_REQUEST.getStatusCode(), resp.getStatus());
            message = resp.readEntity(String.class);
            assertTrue(
                    "Right Exception Message should be returned for missing format",
                    message.contains(
                            "Parameter \"" + fkey + "\" is missing or empty"));
            map.putSingle(fkey, fvalue);
        }
    }


    @Test
    public void testExportWsRTFEmpty () {
        mockClient.reset().when(
            request()
            .withMethod("GET")
            .withPath("/api/v1.0/search")
            .withQueryStringParameter("q", "????")
            )
            .respond(
                response()
                .withHeader("Content-Type: application/json; charset=utf-8")
                .withBody(getFixture("response_none.json"))
                .withStatusCode(200)
                );

        MultivaluedHashMap<String, String> frmap = new MultivaluedHashMap<String, String>();
        frmap.add("format", "rtf");
        frmap.add("q", "????");
        frmap.add("ql", "poliqarp");
        String filenamer = "dateiRtf";
        frmap.putSingle("fname", filenamer);

        String message;

        Response responsertf = target("/export").request()
            .post(Entity.form(frmap));
        assertEquals("Request RTF: Http Response should be 200: ",
                Status.OK.getStatusCode(), responsertf.getStatus());
    }


    @Test
    public void testExportWsProxyProblem () {
        ExWSConf.properties(null).setProperty("api.port", String.valueOf(mockServer.getPort() + 11));

        String filenamej = "dateiJson";
        MultivaluedHashMap<String, String> frmap = new MultivaluedHashMap<String, String>();
        frmap.add("fname", filenamej);
        frmap.add("format", "json");
        frmap.add("q", "????");
        frmap.add("ql", "poliqarp");

        String message;

        Response responsejson = target("/export").request()
                .post(Entity.form(frmap));

        ExWSConf.properties(null).setProperty("api.port", String.valueOf(mockServer.getPort()));

        assertEquals("Request JSON: Http Response should be 502: ",
                     Status.BAD_GATEWAY.getStatusCode(), responsejson.getStatus());

        String str = responsejson.readEntity(String.class);
        assertTrue("HTTP Body", str.contains("P.log(502, 'Unable to reach Backend');"));
    };

    
    // Get fixture from ressources
    private String getFixture (String file) {
        String filepath = getClass()
            .getResource("/fixtures/" + file)
            .getFile();
        return getFileString(filepath);
    };
    

    // Get string from a file
    public static String getFileString (String filepath) {
        StringBuilder contentBuilder = new StringBuilder();
        try {			
			BufferedReader in = new BufferedReader(
				new InputStreamReader(
					new FileInputStream(URLDecoder.decode(filepath, "UTF-8")),
					"UTF-8"
					)
				);
            String str;
            while ((str = in.readLine()) != null) {
                contentBuilder.append(str);
            };
            in.close();
        }
        catch (IOException e) {
            fail(e.getMessage());
        }
        return contentBuilder.toString();
    };
};
