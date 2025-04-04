package de.ids_mannheim.korap.plkexport;

import org.junit.Test;
import static org.junit.Assert.*;

import java.util.Properties;

import org.glassfish.jersey.server.ResourceConfig;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.mockserver.client.MockServerClient;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.Header;
import org.mockserver.model.Parameter;

import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.Response;

import static org.mockserver.model.HttpRequest.*;
import static org.mockserver.model.HttpResponse.*;

/**
 * @author Helge
 */

public class PluginAuthTest extends ExpTest {
    
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

        // Unfortunately this means the tests can't run in parallel
        mockServer = ClientAndServer.startClientAndServer(34765);
        mockClient = new MockServerClient("localhost", mockServer.getPort());
    }

    @Before
    public void resetProps () {
        Properties properties = ExWSConf.properties(null);
        properties.setProperty("api.host", "localhost");
        properties.setProperty("api.port", String.valueOf(mockServer.getPort()));
      
        //Needed for PluginAuth
        properties.setProperty("idp.port", String.valueOf(mockServer.getPort()));
        properties.setProperty("idp.scheme", "http");
        properties.setProperty("idp.host","localhost");

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


    /*
     * Tests if PluginAuth Object is correctly initalized with client ID and client secret.
     */
    @Test 
    public void testPluginInitialize(){
    PluginAuth authi = new PluginAuth("clienttest.properties");
    assertEquals(authi.getClientID(),
    "clitest456");
    assertEquals(authi.getClientSecret(), "clise123");
}

@Test
public void testPluginAuthAuthorise(){
    //TODO: Not quite there yet. Needs rework
        mockClient.reset().when(
        request()
        .withMethod("GET")
        .withPath("/settings/oauth2/authorize")
        .withQueryStringParameter(new Parameter("response_type","code"))
        .withQueryStringParameter(new Parameter("client_id","clitest456"))
        .withQueryStringParameter(new Parameter("scope","search"))
        .withQueryStringParameter(new Parameter("scope","search"))
        )
        .respond(
            response()
            .withStatusCode(307)
        );

    PluginAuth authTest = new PluginAuth("clienttest.properties");
    System.out.println("AuthTest  in Service Test Client-ID: " + authTest.getClientID());
    authTest.authentificate();
     
    //teststate etablieren, sonst kann man das hier nicht abprüfen
  // String path = "/authcode" + "?code=" + "c123" + "&scope =" + "search" + "&state=" + "teststate";
  String path ="/authcode";  
  Response response = target(path).request().get();
  assertEquals(200, response.getStatus());  
}



/**
 * Tests if access token, refresh token and expiry date of access token are 
 * correctly retrieved with authorization code.
 */
// TODO: Is state necessary?
@Test
public void testGetTokenWithAuthCode(){

    mockClient.reset().when(
        request()
        .withMethod("POST")
        .withPath("/api/v1.0/oauth2/token")
        .withHeader("Content-type", "application/x-www-form-urlencoded")
        .withBody("grant_type=authorization_code&client_id=clitest456&client_secret=clise123&code=123") 
        )
        .respond(
            response()
            .withStatusCode(200)
            .withHeader(new Header("Content-Type", "application/json; charset=utf-8"))
            .withBody(getFixture("response_acc_token.json"))
            );

    PluginAuth authTest2 = new PluginAuth("clienttest.properties");    
    authTest2.getToken("authorization_code", "123");
   
    assertEquals("Access_token is set", "4dcf8784ccfd26fac9bdb82778fe60e2", authTest2.getAccessToken());
    assertEquals("Refresh_token is set", "hlWci75xb8atDiq3924NUSvOdtAh7Nlf9z", authTest2.getRefreshToken());
    assertEquals("Expires in ... is se", 259200, authTest2.getExpiresIn());
}


/**
 * Tests if access token, refresh token and expiry date are retrieved correctly with refresh token
 */
@Test
public void testGetTokenWithRefreshToken(){
    mockClient.reset().when(
        request()
        .withMethod("POST")
        .withPath("/api/v1.0/oauth2/token")
        .withHeader("Content-type", "application/x-www-form-urlencoded")
        .withBody("grant_type=refresh_token&client_id=clitest456&client_secret=clise123&refresh_token=4dcf8784ccfd26fac9bdb82778fe60e2") 
        )
        .respond(
            response()
            .withStatusCode(200)
            .withHeader(new Header("Content-Type", "application/json; charset=utf-8"))
            .withBody(getFixture("response_refresh_token.json"))
            );
  
    PluginAuth authTest2 = new PluginAuth("clienttest.properties"); 
    authTest2.getToken("refresh_token", "4dcf8784ccfd26fac9bdb82778fe60e2");
   
    assertEquals("Access_token is set", "trcf8784ccfd26fac9bdb82778fe60e2", authTest2.getAccessToken());
    assertEquals("Refresh_token is set", "trWci75xb8atDiq3924NUSvOdtAh7Nlf9z", authTest2.getRefreshToken());
    assertEquals("Expires in ... is set", 257400, authTest2.getExpiresIn());
}


//TODO Draft Ist not done yet
    @Test
    public void testGetRevokedToken(){
        //TODO hier noch response angucken...
        //TODO bei path noch gucken, ob mit api oder so?
        // Ist das mit header richtig?? Auch oben, Und ist das richtig aufzuschreiben, geht ja ausDoku nicht hervor
          mockClient.reset().when(
            request()
            .withMethod("POST")
            .withPath("/api/v1.0/oauth2/revoke")
            .withHeader("Content-type", "application/x-www-form-urlencoded")
        //    .withBody("client_id=clitest456&client_secret=clise123&token=rtte487&token_type=refresh_token") 
            )
            .respond(
                response()
                .withStatusCode(200)
                );
  
        PluginAuth authTest3 = new PluginAuth("clienttest.properties");
        boolean revoked = authTest3.revokeToken("rtte487");
      
        assertEquals("Refresh token is revoked", true, revoked);

    }

}
