/**
 * 
 * @author helge
 * 
 * Class to define the constants of the export web service, 
 * like the maximum hits to be exported
 *
 */
package de.ids_mannheim.korap.plkexport;

import java.io.*;
import java.lang.String;
import java.util.Properties;

public class ExWSConf {
    /*
     * maximum hits to be exported
     * TODO: Define this constants after discussing it. 
     * Maybe we need a distinction between users at the IDS and external users
     * See also: https://www.ids-mannheim.de/cosmas2/script-app/hilfe/sitzung.html
     */
    public static final int MAX_EXP_LIMIT = 10000;
    /* 
     * TODO:
     * Analog zur Variable aus Search.pm 
     * Kommentar eventuell JSON um aus Perl und Java einzulesen 
     */
    // Eigentlich 25 zu Testzwecken kleiner
    // public static final int PAGE_SIZE = 25;
    public static final int PAGE_SIZE = 5;
    
    // Version of Export Plugin
    public static final int VERSION_MAJOR = 0;
    public static final int VERSION_MINOR = 1;
    public static final int VERSION_PATCHLEVEL= 1;

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
            prop.load(iFile);
        }
        catch (IOException t) {
            try {
                iFile = ExWSConf.class.getClassLoader()
                    .getResourceAsStream(propFile);

                if (iFile == null) {
                    System.err.println("Unable to load properties.");
                    return null;
                };

                prop = new Properties();
                prop.load(iFile);
                iFile.close();
            }
            catch (IOException e) {
                System.err.println(e.getLocalizedMessage());
                return null;
            };
        };
        return prop;
    };
}
