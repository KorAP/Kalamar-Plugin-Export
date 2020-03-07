package de.ids_mannheim.korap.plkexport;

import static org.junit.Assert.assertEquals;

import javax.ws.rs.core.Application;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response.Status;
import java.net.URI;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.junit.Test;

import de.ids_mannheim.korap.plkexport.IdsExportService;


public class IdsExportServiceTest extends JerseyTest {
	 
	   @Override
	    protected Application configure() {
	        return new ResourceConfig(IdsExportService.class);
	    }


	   @Test
	   public void testExportWs() {
           WebTarget t = target("/ids-export/exportHtml");
		   Response response = t.request().get();
		   assertEquals("Http Response should be 200: ", Status.OK.getStatusCode(), response.getStatus());
		   assertEquals("Http Content-Type should be: ", MediaType.TEXT_HTML, response.getHeaderString(HttpHeaders.CONTENT_TYPE));
		   String content = response.readEntity(String.class);
		   assertEquals("Content of response is: ", "Export Web Service under construction", content);

           URI uri = t.getUri();
           assertEquals("Host is: ", "localhost", uri.getHost());

           // Expected: 7777 or 7070
           assertEquals("Port is: ", 7777, uri.getPort());
           assertEquals("URI is: ", "http://localhost:7777/ids-export/exportHtml", uri.toString());
       }

}

