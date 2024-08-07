package de.ids_mannheim.korap.plkexport;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

// Mockserver tests
import org.mockserver.integration.ClientAndServer;
import org.mockserver.client.MockServerClient;
import static org.mockserver.model.HttpRequest.*;
import static org.mockserver.model.HttpResponse.*;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.AfterClass;
import org.mockserver.model.Header;

// Fixture loading
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URLDecoder;

import java.util.LinkedList;
import java.util.Properties;

import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

// SSE testing
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import org.glassfish.jersey.media.sse.EventListener;
import org.glassfish.jersey.media.sse.EventSource;
import org.glassfish.jersey.media.sse.SseFeature;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.fasterxml.jackson.core.JsonParser;

import static de.ids_mannheim.korap.plkexport.Service.getClientIP;


public class ServiceTest extends JerseyTest {

    private static ClientAndServer mockServer;
	private static MockServerClient mockClient;
    private ObjectMapper mapper = new ObjectMapper();

    // Cell split for RTF info table
    private static final String CELLSPLIT = "\\cell\\cf0\\fs18\\b0\\f1 ";

    @BeforeClass
    public static void startServer() {
        // Define logging rules for Mock-Server
        ((ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory
         .getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME))
            .setLevel(ch.qos.logback.classic.Level.OFF);
        ((ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory
         .getLogger("org.mockserver"))
            .setLevel(ch.qos.logback.classic.Level.OFF);

        // Unfortunately this means the tests can't run in parallel
        mockServer = ClientAndServer.startClientAndServer(34765);
        mockClient = new MockServerClient("localhost", mockServer.getPort());
    }

    @Before
    public void resetProps () {
        Properties properties = ExWSConf.properties(null);
        properties.setProperty("api.host", "localhost");
        properties.setProperty("api.port", String.valueOf(mockServer.getPort()));
        properties.setProperty("api.scheme", "http");
        properties.remove("api.source");
    }
   
    @AfterClass
    public static void stopServer() {
        mockServer.stop();
    }
    
    @Override
    protected Application configure () {
        return new ResourceConfig(Service.class);
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
                .withHeaders(new Header("Content-Type", "application/json; charset=UTF-8"))
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
    public void testExportWsJsonEmpty () {
        mockClient.reset().when(
            request()
            .withMethod("GET")
            .withPath("/api/v1.0/search")
            .withQueryStringParameter("q", "axdjakdftr")
            )
            .respond(
                response()
                .withStatusCode(200)
                .withHeaders(new Header("Content-Type", "application/json; charset=UTF-8"))
                .withBody(getFixture("response_none.json"))
                );

        String filenamej = "dateiJson";
        MultivaluedHashMap<String, String> frmap = new MultivaluedHashMap<String, String>();
        frmap.add("fname", filenamej);
        frmap.add("format", "json");
        frmap.add("q", "axdjakdftr");
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
                .withHeaders(new Header("Content-Type", "application/json; charset=UTF-8"))
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

        Properties properties = ExWSConf.properties(null);
        properties.setProperty("api.source", "my-server");

        mockClient.reset().when(
            request()
            .withMethod("GET")
            .withPath("/api/v1.0/search")
            .withQueryStringParameter("q", "Fragerunde")
            .withQueryStringParameter("cq", "corpusSigle = \"WPD17\"")
            )
            .respond(
                response()
                .withHeaders(new Header("Content-Type", "application/json; charset=UTF-8"))
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
        assertTrue("Corpus info", str.contains("Corpus:"));
        assertTrue("Corpus def", str.contains("corpusSigle = \"WPD17\""));
        assertFalse("Errors", str.contains("dynCall("));
        assertTrue("Source info", str.contains("Source:" + CELLSPLIT + "my-server"));
        
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
                .withStatusCode(200)
                .withHeaders(new Header("Content-Type", "application/json; charset=UTF-8"))
                .withBody(getFixture("response_broken.json"))
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
        assertTrue("Error", str.contains("line: 1, column: "));

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
                .withHeaders(new Header("Content-Type", "application/json; charset=UTF-8"))
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
                .withHeaders(new Header("Content-Type", "application/json; charset=UTF-8"))
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
        assertTrue("Error", str.contains("line: 1, column: "));
    }
    

    @Test
    public void testExportWsRTFEmpty () {
        mockClient.reset().when(
            request()
            .withMethod("GET")
            .withPath("/api/v1.0/search")
            .withQueryStringParameter("q", "axdjakdftr")
            )
            .respond(
                response()
                .withStatusCode(200)
                .withHeaders(new Header("Content-Type", "application/json; charset=UTF-8"))
                .withBody(getFixture("response_none.json"))
                );

        MultivaluedHashMap<String, String> frmap = new MultivaluedHashMap<String, String>();
        frmap.add("format", "rtf");
        frmap.add("q", "axdjakdftr");
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
                .withHeaders(new Header("Content-Type", "application/json; charset=UTF-8"))
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
                .withHeaders(new Header("Content-Type", "application/json; charset=UTF-8"))
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
        assertTrue("TotalResults", str.contains("Number of results:"));
        assertFalse("Fetched", str.contains("Fetched:"));

        frmap.putSingle("hitc", "7");

        responsertf = target("/export").request()
            .post(Entity.form(frmap));
        assertEquals("Request RTF: Http Response should be 200: ",
                Status.OK.getStatusCode(), responsertf.getStatus());

        str = responsertf.readEntity(String.class);
        assertTrue("Page 1 content", str.contains("Ironhoof"));
        assertTrue("Page 2 content", str.contains("Sinologie"));
        assertTrue("Unicode handling", str.contains("\\u252\\'fcbersetzt"));
        assertTrue("TotalResults", str.contains("Number of results:" + CELLSPLIT + "9\\cell"));
        assertTrue("Fetched", str.contains("Fetched:" + CELLSPLIT + "7\\cell"));
        assertTrue("Source", str.contains("Source:" + CELLSPLIT + "localhost\\cell"));
        assertTrue("Backend-Version", str.contains("Backend-Version:" + CELLSPLIT + "0.59.2"));
        assertTrue("Export-Version", str.contains("Export-Version:" + CELLSPLIT));
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
                .withHeaders(new Header("Content-Type", "application/json; charset=UTF-8"))
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
                .withHeaders(new Header("Content-Type", "application/json"))
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
        assertTrue("Unicode handling "+str, str.contains("Hintergr\\u252\\'fcnde"));
        assertTrue("TotalResults", str.contains("> 22 ("));
    };
    

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
                .withHeaders(new Header("Content-Type", "application/json; charset=UTF-8"))
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
                .withHeaders(new Header("Content-Type", "application/json; charset=UTF-8"))
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
        assertEquals("Request JSON: Http Response should be 200: ",
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


        frmap.putSingle("hitc", "7");

        responsejson = target("/export").request()
            .post(Entity.form(frmap));
        assertEquals("Request JSON: Http Response should be 200: ",
                Status.OK.getStatusCode(), responsejson.getStatus());

        str = responsejson.readEntity(String.class);
        parser = mapper.getFactory().createParser(str);
        obj = mapper.readTree(parser);

        assertEquals(obj.at("/query/@type").asText(),"koral:token");
        assertEquals(obj.at("/meta/totalResults").asInt(),9);
        assertEquals(obj.at("/matches/0/matchID").asText(),"match-WUD17/G59/34284-p4238-4239");
        assertEquals(obj.at("/matches/1/matchID").asText(),"match-WUD17/C53/60524-p736-737");
        assertEquals(obj.at("/matches/6/matchID").asText(),"match-WUD17/K35/39955-p16114-16115");
        assertFalse(obj.has("/matches/7"));
    };


    @Test
    public void testExportWsCsvPaging () throws IOException {

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
                .withHeaders(new Header("Content-Type", "application/json; charset=UTF-8"))
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
                .withHeaders(new Header("Content-Type", "application/json; charset=UTF-8"))
                .withBody(getFixture("response_plagegeist_1.json"))
                .withStatusCode(200)
                );
        
        MultivaluedHashMap<String, String> frmap = new MultivaluedHashMap<String, String>();
        frmap.add("format", "csv");
        frmap.add("q", "Plagegeist");
        frmap.add("ql", "poliqarp");
        frmap.add("hitc", "30");
        String filenamer = "dateiPagingCsv";
        frmap.putSingle("fname", filenamer);

        Response responsecsv = target("/export").request()
            .post(Entity.form(frmap));
        assertEquals("Request CSV: Http Response should be 200: ",
                Status.OK.getStatusCode(), responsecsv.getStatus());

        String str = responsecsv.readEntity(String.class);
        String[] lines = str.split("\n");

        assertEquals(lines.length,10);
        assertEquals("HasMoreLeft,leftContext,Match,rightContext,HasMoreRight,isCutted,textSigle,author,pubDate,title", lines[0]);
        assertEquals("...,\"1 Tag gesperrt. 24h Urlaub.^^ LG;--  17:40, 11. Jan. 2011 (CET) Danke ich habe die nahezu zeitgleichen VMs von Dir und Ironhoof gesehen. Ob es ein Grund zum Jubeln ist, sei dahin gestellt. Immerhin habe ich für 24 Stunden einen \"\"\",Plagegeist,\" \"\" weniger. Sag mal, zum Kölner Stammtisch isses doch nicht so weit ... wie wär's ? Besten  17:49, 11. Jan. 2011 (CET) Er wurde gesperrt. Nach dem Theater hier zurecht. ABER: auch deine Beiträge hier, die er versuchte zu löschen, sorgen nicht für\",...,,WUD17/G59/34284,\"Umherirrender, u.a.\",2017-07-01,\"Benutzer Diskussion:Gruß Tom/Archiv/2011\"", lines[1]);

        frmap.putSingle("hitc", "7");

        responsecsv = target("/export").request()
            .post(Entity.form(frmap));
        assertEquals("Request CSV: Http Response should be 200: ",
                Status.OK.getStatusCode(), responsecsv.getStatus());

        str = responsecsv.readEntity(String.class);
        lines = str.split("\n");

        assertEquals(lines.length,8);
        assertEquals(lines[0],"HasMoreLeft,leftContext,Match,rightContext,HasMoreRight,isCutted,textSigle,author,pubDate,title");
        assertEquals(lines[7],"...,\"vielleicht eine neue Schloss-Einstein-Antragswelle unterbinden.-- 07:36, 23. Jun. 2008 (CEST)  Mentor  Lieber Kriddl, als ich mir die Liste der Mentoren anschaute, fiel mein Augenmerk auf Dich als Jurist. Könntest Du mir jungen Wikipedianer (aber nicht jung an Jahren) helfen, einen\",Plagegeist,\", der mich seit meiner ersten Teilnahme als IP mobbt, helfen? Wenn ja, so schau Dir doch als Einstieg bitte meinen Wiederherstellungs-Antrag zum Artikel Meton-Periode an: WP:LP, 26.Juni 08. Dort ist nicht nur der Sachverhalt, in den man sich nicht\",...,,WUD17/K35/39955,\"TaxonBot, u.a.\",2017-07-01,\"Benutzer Diskussion:Kriddl/Archiv\"");

        // Check for pageSize adjustments
        frmap.putSingle("hitc", "2");

        responsecsv = target("/export").request()
            .post(Entity.form(frmap));
        assertEquals("Request CSV: Http Response should be 200: ",
                Status.OK.getStatusCode(), responsecsv.getStatus());

        str = responsecsv.readEntity(String.class);
        lines = str.split("\n");

        assertEquals(lines.length,3);
    };
    

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
                .withHeaders(new Header("Content-Type", "application/json; charset=UTF-8"))
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
                .withHeaders(new Header("Content-Type", "application/json; charset=UTF-8"))
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
    public void testExportWsProgressError () throws InterruptedException {

        final ConcurrentLinkedQueue<String> events = new ConcurrentLinkedQueue<>();
        // final int eventCount = 3;

        // Expect messages:
        // final CountDownLatch latch = new CountDownLatch(eventCount);
        
        // Create SSE client
        Client client = ClientBuilder
            .newBuilder()
            .register(SseFeature.class)
            .build();

        EventSource eventSource = EventSource
            .target(target("/export"))
            .reconnectingEvery(300, TimeUnit.SECONDS)
            .build();

        EventListener listener = inboundEvent -> {
            events.add(inboundEvent.getName() + ":" + inboundEvent.readData(String.class));
            // latch.countDown();
        };

        eventSource.register(listener);
        eventSource.open();

        // latch.await(1000, TimeUnit.SECONDS);
        Thread.sleep(2000);

        // Check error
        assertEquals(events.poll(), "Process:init");
        assertEquals(events.poll(), "Error:HTTP 400 Bad Request");
        assertEquals(events.poll(), "Process:done");
        assertTrue(events.isEmpty());
        eventSource.close();
    };


    @Test
    public void testExportWsProgress () throws InterruptedException {
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
                .withHeaders(new Header("Content-Type", "application/json; charset=UTF-8"))
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
                .withHeaders(new Header("Content-Type", "application/json; charset=UTF-8"))
                .withBody(getFixture("response_plagegeist_1.json"))
                .withStatusCode(200)
                );

        // Based on https://stackoverflow.com/questions/35499655/
        //   how-to-test-server-sent-events-with-spring
        //   &
        // https://github.com/jersey/jersey/blob/master/examples/
        //   server-sent-events-jersey/src/test/java/org/glassfish/
        //   jersey/examples/sse/jersey/ServerSentEventsTest.java
        final ConcurrentLinkedQueue<String> events = new ConcurrentLinkedQueue<>();


        // Create SSE client
        Client client = ClientBuilder
            .newBuilder()
            .register(SseFeature.class)
            .build();

        EventSource eventSource = EventSource
            .target(target("/export")
                    .queryParam("q", "Plagegeist")
                    .queryParam("ql","poliqarp")
                    .queryParam("format","rtf"))
            .reconnectingEvery(300, TimeUnit.SECONDS)
            .build();

        EventListener listener = inboundEvent -> {
            events.add(inboundEvent.getName() + ":" + inboundEvent.readData(String.class));
        };

        eventSource.register(listener);
        eventSource.open();

        Thread.sleep(3000);

        // Check error
        assertEquals(events.poll(), "Process:init");
        assertTrue(events.contains("Progress:0"));
        assertTrue(events.contains("Progress:56"));
        events.poll();
        events.poll();
        assertTrue(events.peek().startsWith("Relocate:"));
        assertTrue(events.peek().contains(";"));

        String rel = events.poll();

        assertEquals(events.poll(), "Process:done");
        assertTrue(events.isEmpty());
        eventSource.close();

        // Now fetch the file!
        String[] splits = rel.substring(9).split(";");
        String fileLoc = splits[0];        
        String filename = splits[1];
        Response response = target("/export/" + fileLoc).queryParam("fname", filename).request().get();

        String str = response.readEntity(String.class);
        
        assertEquals("HTTP Code",
                     Status.OK.getStatusCode(), response.getStatus());
        assertTrue(
            "Request: Results should not be displayed inline, but saved and displayed locally",
            response.getHeaderString(HttpHeaders.CONTENT_DISPOSITION)
            .contains("attachment"));

        assertTrue("Request: Filename should be set correctly: ",
                   response.getHeaderString(HttpHeaders.CONTENT_DISPOSITION)
                   .contains("filename=" + filename + ".rtf"));

        // An RTF document should be returned
        assertEquals("Request RTF: Http Content-Type should be: ",
                "application/rtf",
                response.getHeaderString(HttpHeaders.CONTENT_TYPE));

        
        assertTrue("Intro", str.contains("{\\rtf1\\ansi\\deff0"));
        assertTrue("Outro", str.contains("{\\pard\\brdrb\\brdrs\\brdrw2\\brsp20\\par}"));
        assertTrue("Content", str.contains("Benutzer Diskussion:Kriddl"));


        // Check, that the file was succesfully removed after downloading
        response = target("/export/" + fileLoc).queryParam("fname", filename).request().get();
        assertEquals("HTTP Code", Status.NOT_FOUND.getStatusCode(), response.getStatus());
    };


    @Test
    public void testExportWsProgressSinglePage () throws InterruptedException {
        mockClient.reset().when(
            request()
            .withMethod("GET")
            .withPath("/api/v1.0/search")
            )
            .respond(
                response()
                .withHeaders(new Header("Content-Type", "application/json; charset=UTF-8"))
                .withBody(getFixture("response_water.json"))
                .withStatusCode(200)
                );

        final LinkedList<String> events = new LinkedList<>();

        // Create SSE client
        Client client = ClientBuilder
            .newBuilder()
            .register(SseFeature.class)
            .build();

        EventSource eventSource = EventSource
            .target(target("/export")
                    .queryParam("q", "Wasser")
                    .queryParam("ql","poliqarp")
                    .queryParam("hitc","1")
                    .queryParam("format","json"))
            .reconnectingEvery(300, TimeUnit.SECONDS)
            .build();

        EventListener listener = inboundEvent -> {
            events.add(inboundEvent.getName() + ":" + inboundEvent.readData(String.class));
        };

        eventSource.register(listener);
        eventSource.open();

        Thread.sleep(3000);

        // Check error
        assertEquals(events.getFirst(), "Process:init");
        assertEquals(events.get(1), "Progress:0");
        assertTrue(events.get(2).startsWith("Relocate:"));       
        assertEquals(events.getLast(), "Process:done");
        assertEquals(events.size(), 4);
        eventSource.close();

        String fileLoc = events.get(2).substring(9);
        assertTrue(fileLoc.length() > 5);
        assertTrue(fileLoc.contains(";"));
    };
    

    @Test
    public void testFileServingError () {
        String fileLoc = "hjGHJghjgHJGhjghj";
        Response response = target("/export/" + fileLoc).request().get();

        assertEquals("HTTP Code",
                     Status.NOT_FOUND.getStatusCode(), response.getStatus());
        
    };
    
    
    @Test
    public void testClientIP () {
        assertEquals(getClientIP("10.0.4.6"), "10.0.4.6");
        assertEquals(getClientIP("10.0.4.5, 10.0.4.6"), "10.0.4.6");
        assertEquals(getClientIP("10.0.4.6, 10.0.4.256"), "10.0.4.6");
        assertEquals(getClientIP("10.0.4.6, 256.0.4.6 , 10.0.4.256"), "10.0.4.6");
        assertEquals(getClientIP("10.0.4.6, 14.800.4.6 , 10.0.4.256"), "10.0.4.6");
    };


    // Get fixture from ressources utf8 encoded
    private String getFixture (String file) {
        return getFixture(file, true);
    }

    
    // Get fixture from ressources as byte stream
    private String getFixture (String file, Boolean raw) {
        String filepath = getClass()
            .getResource("/fixtures/" + file)
            .getFile();

        if (raw) {
            return getFileString(filepath);
        };
        return Util.convertToUTF8(getFileString(filepath));
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
