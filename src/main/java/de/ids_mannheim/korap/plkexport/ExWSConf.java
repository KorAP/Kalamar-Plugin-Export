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
    // Load properties from file
    public static Properties properties (String propFile) {

         if (prop != null)
            return prop;

        if (propFile == null)
            propFile = "exportPlugin.conf";

        InputStream iFile;
        try {

            iFile = new FileInputStream(propFile);     
            prop = new Properties();
            prop.load(
                new BufferedReader(
                    new InputStreamReader(iFile, "UTF-8")
                    )
                );
        }
        catch (IOException t) {
            try {
                iFile = ExWSConf.class.getClassLoader()
                    .getResourceAsStream(propFile);

                if (iFile == null) {
                    Logger.error("Unable to load properties.");
                    return null;
                };

                prop = new Properties();
                prop.load(iFile);
                iFile.close();
            }
            catch (IOException e) {
                Logger.error(e);
                return null;
            };
        };
        return prop;
    };
}
