package de.ids_mannheim.korap.plkexport;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.server.ResourceConfig;
import java.util.Properties;
import jakarta.ws.rs.core.Application;

/**
 * @author Helge
 * 
 * Tests if export template is correctly displayed
 */
public class ExpTemlTest extends JerseyTest {
   
    @Override
    protected Application configure () {
        return new ResourceConfig(Service.class);
    }

    /*
     * Tests if export template is produced from service /export/template
     */
    @Test
    public void testTemplService(){
        Response response = target("/export/template").request()
                .get();
        assertEquals("Http Response should be 200:",
                Status.OK.getStatusCode(), response.getStatus());
        String json = response.readEntity(String.class);
        assertTrue(json.contains("\"name\" : \"Export\""));
        assertTrue(json.contains(" \"classes\" : [ \"button-icon\", \"plugin\" ]"));
        Properties properties = ExWSConf.properties(null);
        String templurl= properties.getProperty("server.scheme") + "://" +  properties.getProperty("server.host")
        + ":" + properties.getProperty("server.port") + "/export";
      assertTrue(json.contains(templurl));
    }

    /*
     * Tests if exportTemplate is returned correctly
     */
    @Test 
    public void testGetTempl(){
        String scheme = "httpx";
        String host = "xlocalhost";
        String port = "1234";
        String json = ExpTempl.getExportTempl("httpx", "xlocalhost", "1234");
        assertTrue(json.contains("\"desc\" : \"Exports Kalamar results\""));
        assertTrue(json.contains( "\"title\" : \"exports KWICs and snippets\""));
        assertTrue(json.contains( "\"title\" : \"exports KWICs and snippets\""));
        String templ = "\"template\" : \""+ scheme +"://"+host + ":" + port + "/export\"";
        assertTrue(json.contains(templ));
    }
}
