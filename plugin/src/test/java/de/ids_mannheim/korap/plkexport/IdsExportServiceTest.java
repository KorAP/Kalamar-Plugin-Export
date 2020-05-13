package de.ids_mannheim.korap.plkexport;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

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

/*
 * TODO Find a way to test efficiently the starting of the PluginServer with host and port of the config-file
 * + serving static files
 */

public class IdsExportServiceTest extends JerseyTest {

    @Override
    protected Application configure () {
        return new ResourceConfig(IdsExportService.class);
    }


    // Client is pre-configured in JerseyTest
    /**
     * Tests if webservice returns a document with the right filename
     * and file
     * format.
     */
    @Test
    public void testExportWs () {

        String filenamej = "dateiJson";
        String filenamer = "dateiRtf";
        MultivaluedHashMap<String, String> frmap = new MultivaluedHashMap<String, String>();
        frmap.add("fname", filenamej);
        frmap.add("format", "json");
        frmap.add("q", "Wasser");
        frmap.add("ql", "poliqarp");

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

        frmap.putSingle("format", "rtf");
        frmap.putSingle("fname", filenamer);

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
                responsejson.getHeaderString(HttpHeaders.CONTENT_DISPOSITION)
                        .contains("attachment"));
        // The document should be named correctly
        assertTrue("Request RTF: Filename should be set correctly: ",
                responsertf.getHeaderString(HttpHeaders.CONTENT_DISPOSITION)
                        .contains("filename=" + filenamer));
    }

}
