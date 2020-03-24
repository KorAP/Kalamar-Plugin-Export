package de.ids_mannheim.korap.plkexport;

import static org.junit.Assert.assertEquals;

import javax.ws.rs.core.Application;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
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
	    protected Application configure() {
	        return new ResourceConfig(IdsExportService.class);
	    }


	   @Test
	   public void testExportWs() {
		   Response response = target("/ids-export/exportHtml").request().get();
		   assertEquals("Http Response should be 200: ", Status.OK.getStatusCode(), response.getStatus());
		   assertEquals("Http Content-Type should be: ", MediaType.TEXT_HTML, response.getHeaderString(HttpHeaders.CONTENT_TYPE));
		   String content = response.readEntity(String.class);
		   assertEquals("Content of response is: ", "Export Web Service under construction", content);
		  
	   } 
	
}

