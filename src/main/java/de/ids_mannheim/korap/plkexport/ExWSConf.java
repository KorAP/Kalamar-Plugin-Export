/**
 * 
 * @author helge, ndiewald
 * 
 * Class to define the constants of the export web service, 
 * like the maximum hits to be exported
 *
 */
package de.ids_mannheim.korap.plkexport;

import java.io.*;
import java.lang.String;
import java.util.Properties;

import org.tinylog.Logger;

public class ExWSConf {

    // Version of Export Plugin
    private static String VERSION;

    private static Properties prop;

    /*
     * Returns version of the Export Plugin
     */
    public static String version(){
        if (VERSION != null){
            return VERSION;
        }
        else
        {
            Properties projProp = new Properties();
            try{
                projProp.load(ExWSConf.class.getClassLoader().getResourceAsStream("project.properties"));
            }
            catch(Exception e){
                Logger.error("Unable to load project properties");
                return null;
            }
            VERSION = projProp.getProperty("version");
            return VERSION;
    }
    } 
  
    /*
     * Sets properties to null 
     * Is needed for testing purposes.
     */
    public static void clearProp(){
        prop = null;
    }

    /**
    *Loads properties from a UTF-8 encoded file
    */
    public static Properties loadProp(Properties aprop, String file){
    
        InputStream diFile;
        if(aprop ==  null){
            aprop = new Properties();
        }
    
        try {
            diFile = new FileInputStream(file);     
            aprop.load(
                new BufferedReader(
                new InputStreamReader(diFile, "UTF-8")
                )
            );
        }
        catch (IOException t) {
        try {
            diFile = ExWSConf.class.getClassLoader()
                .getResourceAsStream(file);

            if (diFile == null) {
                Logger.error("Unable to load properties.");
                return null;
            };

            aprop.load(diFile);
            diFile.close();
        }
        catch (IOException e) {
            Logger.error(e);
            return null;
        };
    }    
    return aprop;
   }

    /*
    * Returns export properties 
    * The properties in exportPlugin.conf are the default properties
    * which can be overwritten by the properties in propFile.
    */
    public static Properties properties (String propFile) {
     
        if (prop != null)
           return prop;

        Properties defaultProp = loadProp(null, "exportPlugin.conf");
        prop = new Properties(defaultProp);
    
        if (propFile != null){
            loadProp(prop, propFile);
        }
    
        return prop;
    };

}
