/** 
 * @author Helge 
**/

package de.ids_mannheim.korap.plkexport;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.Base64;
import java.util.Properties;
import java.util.Random;


import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Form;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.tinylog.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class PluginAuth {

   private String client_id;
   private String client_secret;

   private String accessToken;
   private String refreshToken;
   
   //TODO: It has to be considered if expires_in is used or 401 if accessToken is no valid anymore
   //expires_in (second) for accessToken
   private int expiresIn;
   //TODO consider scope here or in method authentificate
   private String scope;
   private String state;

 
   String idpPort;
   String idpHost;
   String idpScheme;
 
   public PluginAuth(String clientFile){

        Properties propcli = new Properties();
        String propclFile;

        if (clientFile == null){
            propclFile =  "client.properties";
        }
        else{
            propclFile = clientFile;  
        }
        
        InputStream inStream;
        try {
            FileInputStream iFile = new FileInputStream(propclFile);            
            propcli.load(
                    new InputStreamReader(iFile)
                );   
            iFile.close();
        }

        catch(IOException e){
            try {
                inStream = PluginAuth.class.getClassLoader().getResourceAsStream(propclFile);
                if (inStream == null) {    
                    Logger.error("Unable to load client properties." + e.getMessage());
                }
                else{
                    InputStreamReader inStrRe = new InputStreamReader(inStream);
                    propcli.load(inStrRe);
                    inStream.close();
                }

                } 
            catch (IOException ex) {
                Logger.error(ex);
            }
        }

        this.client_id =  propcli.getProperty("client_id");
        this.client_secret = propcli.getProperty("client_secret");    

        Properties prop = ExWSConf.properties(null);
        idpPort = prop.getProperty("idp.port");
        idpHost = prop.getProperty("idp.host");
        idpScheme = prop.getProperty("idp.scheme");
    }


    public String getClientID(){
        return client_id;
    } 

    public String getClientSecret(){
        return client_secret;
    }

    private void setAccessToken(String token){
        this.accessToken = token;
    }
    
    public String getAccessToken(){
        return this.accessToken;
    }

    private void setRefreshToken(String token){
        this.refreshToken = token;
    }

    public String getRefreshToken(){
        return refreshToken;
    }

    private void setExpiresIn(int sec){
        this.expiresIn = sec;
    }

    public int getExpiresIn(){
        return this.expiresIn;
    } 

    private void setState(String st){
        this.state = st;
    }

    public String getState(){
        return this.state;
    }

    private void setScope(String sc){
        this.scope = sc;
    }

    public String getScope(){
        return this.scope;
    }

    /**
     * Generates state parameter (randomString encoded in Base64)
     */
    public String generateState(){
    byte[] array = new byte[10]; // length is bounded by 7
    new Random().nextBytes(array);
    String randomStr = new String(array, Charset.forName("UTF-8"));
    String state = Base64.getEncoder().encodeToString(randomStr.getBytes());
    return state;
    }


    //TODO This is the first draft and not done at all!!!
    public String authentificate(){

        String clientId = this.getClientID();
        Client clientauthc = ClientBuilder.newClient();
        String path = "/settings/oauth2/authorize";
         
        String responsetype = "code";
        String scope = "search";
        String state = this.generateState();
        String query = "response_type="+ responsetype +"&client_id=" + this.client_id + "&scope=" + scope +"&state="+state;
        String uri ="";

        URI authURI;
      try {
          authURI = new URI(this.idpScheme, null, this.idpHost, Integer.parseInt(this.idpPort), path, query, null);
          uri = authURI.toString();
          System.out.println("URI:" + authURI.toString());
      } catch (URISyntaxException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
      } 
        
          WebTarget webTarget = clientauthc.target(uri);
        
          Invocation.Builder invocationBuilder  = webTarget.request(MediaType.APPLICATION_JSON);
          Response response  = invocationBuilder.get();
 
       //Dummy
        String authCode =  "123";
        return authCode;
      }
      


    //TODO NA grantype enum
   public void getToken(String grantType, String grant) {
        URI uri = URI.create ("");
        String path = "/api/v1.0/oauth2/token";


       String grantparam;
       if(grantType.equals("authorization_code")){
        grantparam = "code";
        }
        else{
            grantparam=grantType;
        }

        try {
           uri = new URI(this.idpScheme, null, this.idpHost, Integer.parseInt(this.idpPort), path, null, null);
           System.out.println("URI in getToken:" + uri.toString());
         } catch (URISyntaxException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        Client clientToken = ClientBuilder.newClient();

        WebTarget webTarget = clientToken.target(uri);
        Invocation.Builder invocationBuilder  = webTarget.request(MediaType.APPLICATION_JSON);
        //String query = "grant_type="+ grantType +"&client_id="+ this.getClientID() + "&client_secret="+ this.getClientSecret() + "&"+grantparam+"=" + grant;
        Form form = new Form().param("grant_type", grantType)
                      .param("client_id",this.getClientID())
                      .param("client_secret",this.getClientSecret())
                      .param(grantparam, grant);
        Response response  = invocationBuilder.post(Entity.form(form));

        
        String jsonString = response.readEntity(String.class);

        ObjectMapper mapper = new ObjectMapper();

        JsonNode node = mapper.createObjectNode();
    
        try {
            node = mapper.readTree(jsonString);
        } catch (JsonProcessingException e) {
            Logger.error(e);
        }
        
        this.setAccessToken(node.get("access_token").asText());
        this.setRefreshToken(node.get("refresh_token").asText());
        this.setExpiresIn(node.get("expires_in").intValue());
       
    }

    public boolean revokeToken(String refreshToken){
        
        String uri="";
        URI authURI;
        String path = "/api/v1.0/oauth2/revoke";
     
        try {
            authURI = new URI(this.idpScheme, null, this.idpHost, Integer.parseInt(this.idpPort), path, null, null);
            uri = authURI.toString();
            System.out.println("URI in revokeToken:" + authURI.toString());
        } catch (URISyntaxException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
     

        Client clientToken = ClientBuilder.newClient();
        WebTarget webTarget = clientToken.target(uri);
        Invocation.Builder invocationBuilder  = webTarget.request(MediaType.APPLICATION_JSON);
       
        Form form = new Form()
        .param("client_id",this.getClientID())
        .param("client_secret",this.getClientSecret())
        .param("token",refreshToken)
        .param("token_type", "refresh_token");
       
        boolean b = false;
        Response response  = invocationBuilder.post(Entity.form(form));
        if(response.getStatus() == 200){
            b = true;
        }
        return b;
    }
}