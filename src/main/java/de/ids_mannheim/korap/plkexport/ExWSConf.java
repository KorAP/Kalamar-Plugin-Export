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
import java.nio.charset.StandardCharsets;

import org.tinylog.Logger;

public class ExWSConf {

    // Version of Export Plugin
    public static final int VERSION_MAJOR = 0;
    public static final int VERSION_MINOR = 2;
    public static final int VERSION_PATCHLEVEL = 5;

    private static Properties prop;

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
