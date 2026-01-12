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
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.function.Function;

import org.tinylog.Logger;

public class ExWSConf {

    // Version of Export Plugin
    private static String VERSION;

    private static Properties prop;

    // Environment provider function (can be overridden for testing)
    private static Function<String, String> envProvider = System::getenv;

    // Mapping from environment variable names to property names
    private static final Map<String, String> ENV_TO_PROP = new HashMap<>();
    static {
        // Server configuration
        ENV_TO_PROP.put("KALAMAR_EXPORT_SERVER_PORT", "server.port");
        ENV_TO_PROP.put("KALAMAR_EXPORT_SERVER_HOST", "server.host");
        ENV_TO_PROP.put("KALAMAR_EXPORT_SERVER_SCHEME", "server.scheme");
        ENV_TO_PROP.put("KALAMAR_EXPORT_SERVER_ORIGIN", "server.origin");
        
        // API configuration
        ENV_TO_PROP.put("KALAMAR_EXPORT_API_PORT", "api.port");
        ENV_TO_PROP.put("KALAMAR_EXPORT_API_HOST", "api.host");
        ENV_TO_PROP.put("KALAMAR_EXPORT_API_SCHEME", "api.scheme");
        ENV_TO_PROP.put("KALAMAR_EXPORT_API_SOURCE", "api.source");
        ENV_TO_PROP.put("KALAMAR_EXPORT_API_PATH", "api.path");
        
        // Asset configuration
        ENV_TO_PROP.put("KALAMAR_EXPORT_ASSET_HOST", "asset.host");
        ENV_TO_PROP.put("KALAMAR_EXPORT_ASSET_PORT", "asset.port");
        ENV_TO_PROP.put("KALAMAR_EXPORT_ASSET_SCHEME", "asset.scheme");
        ENV_TO_PROP.put("KALAMAR_EXPORT_ASSET_PATH", "asset.path");
        
        // General configuration
        ENV_TO_PROP.put("KALAMAR_EXPORT_PAGE_SIZE", "conf.page_size");
        ENV_TO_PROP.put("KALAMAR_EXPORT_MAX_EXP_LIMIT", "conf.max_exp_limit");
        ENV_TO_PROP.put("KALAMAR_EXPORT_FILE_DIR", "conf.file_dir");
        ENV_TO_PROP.put("KALAMAR_EXPORT_DEFAULT_HITC", "conf.default_hitc");
        
        // Cookie configuration
        ENV_TO_PROP.put("KALAMAR_EXPORT_COOKIE_NAME", "cookie.name");
    }

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

    /*
     * Sets a custom environment provider function.
     * This is useful for testing environment variable overrides
     * without actually setting system environment variables.
     * Pass null to reset to the default System.getenv provider.
     */
    public static void setEnvironmentProvider(Function<String, String> provider) {
        if (provider == null) {
            envProvider = System::getenv;
        } else {
            envProvider = provider;
        }
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
    * Environment variables have the highest priority and override both
    * config file values.
    */
    public static Properties properties (String propFile) {
     
        if (prop != null)
           return prop;

        Properties defaultProp = loadProp(null, "exportPlugin.conf");
        prop = new Properties(defaultProp);
    
        if (propFile != null){
            loadProp(prop, propFile);
        }
        
        // Apply environment variable overrides
        applyEnvironmentOverrides(prop);
    
        return prop;
    };

    /*
     * Apply environment variable overrides to the properties.
     * Environment variables have the highest priority.
     */
    private static void applyEnvironmentOverrides(Properties props) {
        for (Map.Entry<String, String> entry : ENV_TO_PROP.entrySet()) {
            String envValue = getEnvironmentVariable(entry.getKey());
            if (envValue != null && !envValue.isEmpty()) {
                props.setProperty(entry.getValue(), envValue);
            }
        }
    }

    /*
     * Get an environment variable value using the configured provider.
     */
    protected static String getEnvironmentVariable(String name) {
        return envProvider.apply(name);
    }

    /*
     * Returns the mapping of environment variable names to property names.
     * Useful for documentation and testing.
     */
    public static Map<String, String> getEnvToPropertyMapping() {
        return new HashMap<>(ENV_TO_PROP);
    }

}
