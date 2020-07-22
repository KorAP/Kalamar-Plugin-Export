/**
 * 
 * @author helge
 * 
 * Class to define the constants of the export web service, 
 * like the maximum hits to be exported
 *
 */
package de.ids_mannheim.korap.plkexport;

public class ExWSConf {
    /*
     * maximum hits to be exported
     * TODO: Define this constants after discussing it. 
     * Maybe we need an distinction between user at the IDS and external user
     * See also: https://www.ids-mannheim.de/cosmas2/script-app/hilfe/sitzung.html
     */
    public static final int MAX_EXP_LIMIT = 10000;
    
    //Version of Export Plugin
    public static final int VERSION_MAJOR = 0;
    public static final int VERSION_MINOR = 1;
    public static final int VERSION_PATCHLEVEL= 0;
}
