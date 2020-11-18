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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import de.ids_mannheim.korap.plkexport.IdsExportService;
import de.ids_mannheim.korap.plkexport.ExWSConf;

import org.eclipse.jetty.server.Request;

import static de.ids_mannheim.korap.plkexport.IdsExportService.getClientIP;

public class IdsExportServiceTest extends JerseyTest {

    private static ClientAndServer mockServer;
	private static MockServerClient mockClient;
    private ObjectMapper mapper = new ObjectMapper();
            
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
        frmap.add("cutoff", "1");
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
                   .contains("filename=" + filenamej + ".json"));

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
    public void testFormHTML2 () {
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
        frmap.add("cutoff", "true");

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
        frmap.add("cutoff", "true");
        String filenamer = "dateiRtf";
        frmap.putSingle("fname", filenamer);

        String message;

        Response responsertf = target("/export").request()
            .post(Entity.form(frmap));
        assertEquals("Request RTF: Http Response should be 200: ",
                Status.OK.getStatusCode(), responsertf.getStatus());
        // An RTF document should be returned
        assertEquals("Request RTF: Http Content-Type should be: ",
                "application/rtf",
                responsertf.getHeaderString(HttpHeaders.CONTENT_TYPE));
        // Results should not be displayed inline but saved and displayed locally
        assertTrue("Request RTF: Results should not be displayed inline",
                responsertf.getHeaderString(HttpHeaders.CONTENT_DISPOSITION)
                        .contains("attachment"));
        // The document should be named correctly
        assertTrue("Request RTF: Filename should be set correctly: ",
                responsertf.getHeaderString(HttpHeaders.CONTENT_DISPOSITION)
                        .contains("filename=" + filenamer + ".rtf"));

        Response resp;
        String fvalue;
        frmap.remove("fname");
        frmap.remove("cutoff");
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
    public void testExportWsRTFcorpusQuery () {
        mockClient.reset().when(
            request()
            .withMethod("GET")
            .withPath("/api/v1.0/search")
            .withQueryStringParameter("q", "Fragerunde")
            .withQueryStringParameter("cq", "corpusSigle = \"WPD17\"")
            )
            .respond(
                response()
                .withHeader("Content-Type: application/json; charset=utf-8")
                .withBody(getFixture("response_fragerunde_1.json"))
                .withStatusCode(200)
                );

        MultivaluedHashMap<String, String> frmap = new MultivaluedHashMap<String, String>();
        frmap.add("format", "rtf");
        frmap.add("q", "Fragerunde");
        frmap.add("ql", "poliqarp");
        frmap.add("cq", "corpusSigle = \"WPD17\"");
        frmap.add("cutoff", "true");
        String filenamer = "dateiRtf";
        frmap.putSingle("fname", filenamer);

        Response responsertf = target("/export").request()
            .post(Entity.form(frmap));
        assertEquals("Request RTF: Http Response should be 200: ",
                Status.OK.getStatusCode(), responsertf.getStatus());
        String str = responsertf.readEntity(String.class);

        assertTrue("Corpus info", str.contains("{\\pard Corpus: \\f1 corpusSigle = \"WPD17\"\\par}"));
        assertFalse("Errors", str.contains("dynCall("));
    }

    @Test
    public void testExportWsRTFbroken () {
        mockClient.reset().when(
            request()
            .withMethod("GET")
            .withPath("/api/v1.0/search")
            )
            .respond(
                response()
                .withHeader("Content-Type: application/json; charset=utf-8")
                .withBody(getFixture("response_broken.json"))
                .withStatusCode(200)
                );

        MultivaluedHashMap<String, String> frmap = new MultivaluedHashMap<String, String>();
        frmap.add("format", "rtf");
        frmap.add("q", "???");
        frmap.add("ql", "poliqarp");
        frmap.add("cq", "corpusSigle = \"WPD17\"");
        frmap.add("cutoff", "true");
        String filenamer = "dateiRtf";
        frmap.putSingle("fname", filenamer);

        Response responsertf = target("/export").request()
            .post(Entity.form(frmap));
        assertEquals("Request RTF: Http Response should be 500: ",
                500, responsertf.getStatus());
        String str = responsertf.readEntity(String.class);

        assertTrue("Title", str.contains("<title>Export</title>"));
        assertTrue("Error", str.contains("line: 1, column: 539"));

        // Check paging with broken second page
        mockClient.reset().when(
            request()
            .withMethod("GET")
            .withPath("/api/v1.0/search")
            .withQueryStringParameter("q", "Plagegeist")
            .withQueryStringParameter("count", "5")
            .withQueryStringParameter("offset", "5")
            )
            .respond(
                response()
                .withHeader("Content-Type: application/json; charset=utf-8")
                .withBody(getFixture("response_broken.json"))
                .withStatusCode(200)
                );

        mockClient.when(
            request()
            .withMethod("GET")
            .withPath("/api/v1.0/search")
            .withQueryStringParameter("q", "Plagegeist")
            )
            .respond(
                response()
                .withHeader("Content-Type: application/json; charset=utf-8")
                .withBody(getFixture("response_plagegeist_1.json"))
                .withStatusCode(200)
                );
        frmap = new MultivaluedHashMap<String, String>();
        frmap.add("format", "rtf");
        frmap.add("q", "Plagegeist");
        frmap.add("ql", "poliqarp");
        frmap.putSingle("fname", filenamer);

        responsertf = target("/export").request()
            .post(Entity.form(frmap));
        assertEquals("Request RTF: Http Response should be 500: ",
                500, responsertf.getStatus());

        str = responsertf.readEntity(String.class);
        assertTrue("Title", str.contains("<title>Export</title>"));
        assertTrue("Error", str.contains("line: 1, column: 539"));
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
        frmap.add("cuttoff", "true");
        String filenamer = "dateiRtf";
        frmap.putSingle("fname", filenamer);

        String message;

        Response responsertf = target("/export").request()
            .post(Entity.form(frmap));

        assertEquals("Request RTF: Http Response should be 200: ",
                Status.OK.getStatusCode(), responsertf.getStatus());
    }

    @Test
    public void testExportWsRTFPaging () {

        mockClient.reset().when(
            request()
            .withMethod("GET")
            .withPath("/api/v1.0/search")
            .withQueryStringParameter("q", "Plagegeist")
            .withQueryStringParameter("count", "5")
            .withQueryStringParameter("offset", "5")
            )
            .respond(
                response()
                .withHeader("Content-Type: application/json; charset=utf-8")
                .withBody(getFixture("response_plagegeist_2.json"))
                .withStatusCode(200)
                );

        mockClient.when(
            request()
            .withMethod("GET")
            .withPath("/api/v1.0/search")
            .withQueryStringParameter("q", "Plagegeist")
            )
            .respond(
                response()
                .withHeader("Content-Type: application/json; charset=utf-8")
                .withBody(getFixture("response_plagegeist_1.json"))
                .withStatusCode(200)
                );
        
        MultivaluedHashMap<String, String> frmap = new MultivaluedHashMap<String, String>();
        frmap.add("format", "rtf");
        frmap.add("q", "Plagegeist");
        frmap.add("ql", "poliqarp");
        frmap.add("hitc", "30");
        String filenamer = "dateiPagingRtf";
        frmap.putSingle("fname", filenamer);

        Response responsertf = target("/export").request()
            .post(Entity.form(frmap));
        assertEquals("Request RTF: Http Response should be 200: ",
                Status.OK.getStatusCode(), responsertf.getStatus());

        // With maxResults
        String str = responsertf.readEntity(String.class);
        assertTrue("Page 1 content", str.contains("Ironhoof"));
        assertTrue("Page 2 content", str.contains("Sinologie"));
        assertTrue("Unicode handling", str.contains("Hintergr\\u252\\'fcnde"));
        assertTrue("TotalResults", str.contains("Count: \\f1 9\\"));
        assertFalse("Fetched", str.contains("Fetched:"));

        frmap.putSingle("hitc", "7");

        responsertf = target("/export").request()
            .post(Entity.form(frmap));
        assertEquals("Request RTF: Http Response should be 200: ",
                Status.OK.getStatusCode(), responsertf.getStatus());

        str = responsertf.readEntity(String.class);
        assertTrue("Page 1 content", str.contains("Ironhoof"));
        assertTrue("Page 2 content", str.contains("Sinologie"));
        assertTrue("Unicode handling", str.contains("Hintergr\\u252\\'fcnde"));
        assertTrue("TotalResults", str.contains("Count: \\f1 9\\"));
        assertTrue("Fetched", str.contains("Fetched: \\f1 7\\"));
    }

    
    @Test
    public void testExportWsRTFPagingWithTimeout () {

        mockClient.reset().when(
            request()
            .withMethod("GET")
            .withPath("/api/v1.0/search")
            .withQueryStringParameter("q", "Plagegeist")
            .withQueryStringParameter("count", "5")
            .withQueryStringParameter("offset", "5")
            )
            .respond(
                response()
                .withHeader("Content-Type: application/json; charset=utf-8")
                .withBody(getFixture("response_plagegeist_2.json"))
                .withStatusCode(200)
                );

        mockClient.when(
            request()
            .withMethod("GET")
            .withPath("/api/v1.0/search")
            .withQueryStringParameter("q", "Plagegeist")
            )
            .respond(
                response()
                .withHeader("Content-Type: application/json; charset=utf-8")
                .withBody(getFixture("response_timeout.json"))
                .withStatusCode(200)
                );
        
        MultivaluedHashMap<String, String> frmap = new MultivaluedHashMap<String, String>();
        frmap.add("format", "rtf");
        frmap.add("q", "Plagegeist");
        frmap.add("ql", "poliqarp");
        frmap.add("hitc", "30");
        String filenamer = "dateiPagingRtf";
        frmap.putSingle("fname", filenamer);

        Response responsertf = target("/export").request()
            .post(Entity.form(frmap));
        assertEquals("Request RTF: Http Response should be 200: ",
                Status.OK.getStatusCode(), responsertf.getStatus());

        String str = responsertf.readEntity(String.class);
        assertTrue("Page 1 content", str.contains("Importwunsch"));
        assertTrue("Page 2 content", str.contains("Sinologie"));
        assertTrue("Unicode handling", str.contains("Hintergr\\u252\\'fcnde"));
        assertTrue("TotalResults", str.contains("Count: \\f1 > 22 ("));
    }


    

    @Test
    public void testExportWsJsonPaging () throws IOException {

        mockClient.reset().when(
            request()
            .withMethod("GET")
            .withPath("/api/v1.0/search")
            .withQueryStringParameter("q", "Plagegeist")
            .withQueryStringParameter("count", "5")
            .withQueryStringParameter("offset", "5")
            )
            .respond(
                response()
                .withHeader("Content-Type: application/json; charset=utf-8")
                .withBody(getFixture("response_plagegeist_2.json"))
                .withStatusCode(200)
                );

        mockClient.when(
            request()
            .withMethod("GET")
            .withPath("/api/v1.0/search")
            .withQueryStringParameter("q", "Plagegeist")
            )
            .respond(
                response()
                .withHeader("Content-Type: application/json; charset=utf-8")
                .withBody(getFixture("response_plagegeist_1.json"))
                .withStatusCode(200)
                );
        
        MultivaluedHashMap<String, String> frmap = new MultivaluedHashMap<String, String>();
        frmap.add("format", "json");
        frmap.add("q", "Plagegeist");
        frmap.add("ql", "poliqarp");
        frmap.add("hitc", "30");
        String filenamer = "dateiPagingJson";
        frmap.putSingle("fname", filenamer);

        Response responsejson = target("/export").request()
            .post(Entity.form(frmap));
        assertEquals("Request RTF: Http Response should be 200: ",
                Status.OK.getStatusCode(), responsejson.getStatus());

        String str = responsejson.readEntity(String.class);
        JsonParser parser = mapper.getFactory().createParser(str);
        JsonNode obj = mapper.readTree(parser);

        assertEquals(obj.at("/query/@type").asText(),"koral:token");
        assertEquals(obj.at("/meta/totalResults").asInt(),9);
        assertEquals(obj.at("/matches/0/matchID").asText(),"match-WUD17/G59/34284-p4238-4239");
        assertEquals(obj.at("/matches/1/matchID").asText(),"match-WUD17/C53/60524-p736-737");
        assertEquals(obj.at("/matches/8/matchID").asText(),"match-WUD17/K35/39955-p16258-16259");
        assertTrue(obj.at("/matches/0/snippet").asText().contains("<span class=\"context-right\">&quot;"));
        assertTrue(obj.at("/matches/0/snippet").asText().contains("wie wär's"));
    }    

    @Test
    public void testExportWsJsonWithMaxHitcFirstPage () throws IOException {

        // This should ensure here to check that page 2 is not loaded
        mockClient.reset().when(
            request()
            .withMethod("GET")
            .withPath("/api/v1.0/search")
            .withQueryStringParameter("q", "Plagegeist")
            .withQueryStringParameter("count", "5")
            .withQueryStringParameter("offset", "5")
            )
            .respond(
                response()
                .withHeader("Content-Type: application/json; charset=utf-8")
                .withBody(getFixture("response_broken.json"))
                .withStatusCode(200)
                );

        mockClient.when(
            request()
            .withMethod("GET")
            .withPath("/api/v1.0/search")
            .withQueryStringParameter("q", "Plagegeist")
            )
            .respond(
                response()
                .withHeader("Content-Type: application/json; charset=utf-8")
                .withBody(getFixture("response_plagegeist_1.json"))
                .withStatusCode(200)
                );

        MultivaluedHashMap<String, String> frmap = new MultivaluedHashMap<String, String>();
        frmap.add("format", "json");
        frmap.add("q", "Plagegeist");
        frmap.add("ql", "poliqarp");
        frmap.add("hitc", "3");
        String filenamer = "dateiPagingJson";
        frmap.putSingle("fname", filenamer);

        Response responsejson = target("/export").request()
            .post(Entity.form(frmap));
        assertEquals("Request RTF: Http Response should be 200: ",
                Status.OK.getStatusCode(), responsejson.getStatus());

        String str = responsejson.readEntity(String.class);
        JsonParser parser = mapper.getFactory().createParser(str);
        JsonNode obj = mapper.readTree(parser);

        assertEquals(obj.at("/query/@type").asText(),"koral:token");
        assertEquals(obj.at("/meta/totalResults").asInt(),9);
        assertEquals(obj.at("/matches/0/matchID").asText(),"match-WUD17/G59/34284-p4238-4239");
        assertEquals(obj.at("/matches/1/matchID").asText(),"match-WUD17/C53/60524-p736-737");
        assertEquals(obj.at("/matches/2/matchID").asText(),"match-WUD17/J34/49397-p19826-19827");
        assertFalse(obj.has("/matches/3"));
        assertTrue(obj.at("/matches/0/snippet").asText().contains("<span class=\"context-right\">&quot;"));
        assertTrue(obj.at("/matches/0/snippet").asText().contains("wie wär's"));
    }    
    

    @Test
    public void testExportWsProxyProblem () {
        Properties properties = ExWSConf.properties(null);
        String portTemp = properties.getProperty("api.port");
        properties.setProperty("api.port", String.valueOf(mockServer.getPort() + 11));

        String filenamej = "dateiJson";
        MultivaluedHashMap<String, String> frmap = new MultivaluedHashMap<String, String>();
        frmap.add("fname", filenamej);
        frmap.add("format", "json");
        frmap.add("q", "????");
        frmap.add("ql", "poliqarp");
        frmap.add("cuttoff", "true");

        Response responsejson = target("/export").request()
                .post(Entity.form(frmap));

        ExWSConf.properties(null).setProperty("api.port", String.valueOf(mockServer.getPort()));

        assertEquals("Request JSON: Http Response should be 502: ",
                     Status.BAD_GATEWAY.getStatusCode(), responsejson.getStatus());

        String str = responsejson.readEntity(String.class);
        assertTrue("HTTP Body", str.contains("P.log(502, 'Unable to reach Backend');"));
        properties.setProperty("api.port", portTemp);
    };


    @Test
    public void testClientIP () {
        assertEquals(getClientIP("10.0.4.6"), "10.0.4.6");
        assertEquals(getClientIP("10.0.4.5, 10.0.4.6"), "10.0.4.6");
        assertEquals(getClientIP("10.0.4.6, 10.0.4.256"), "10.0.4.6");
        assertEquals(getClientIP("10.0.4.6, 256.0.4.6 , 10.0.4.256"), "10.0.4.6");
        assertEquals(getClientIP("10.0.4.6, 14.800.4.6 , 10.0.4.256"), "10.0.4.6");
    };


    // Convert string to utf8
    private static String convertToUTF8(String s) {
        String out = null;
        try {
            out = new String(s.getBytes("UTF-8"), "ISO-8859-1");
        } catch (java.io.UnsupportedEncodingException e) {
            return null;
        }
        return out;
    }

    // Get fixture from ressources utf8 encoded
    private String getFixture (String file) {
        return getFixture(file, false);
    };

    
    // Get fixture from ressources as byte stream
    private String getFixture (String file, Boolean raw) {
        String filepath = getClass()
            .getResource("/fixtures/" + file)
            .getFile();

        if (raw) {
            return getFileString(filepath);
        };
        return convertToUTF8(getFileString(filepath));
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
