package de.ids_mannheim.korap.plkexport;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Properties;

import org.junit.Test;

import de.ids_mannheim.korap.plkexport.ExWSConf;

public class ExWSConfTest {

    /**
     * Test the return of properties.
     * If no property file is passed, the default properties
     * should be loaded. If there is a property file, the properties in 
     * this file should additionall be loaded. Default properties should be
     * overwritten if they exist in both file.
     */
    @Test 
    public void testExportWsOverlProp () {
        // No property file
        ExWSConf.clearProp();
        Properties properties = ExWSConf.properties(null);
        assertEquals("localhost", (properties.getProperty("server.host")));
        assertEquals("1024", (properties.getProperty("server.port")));
        assertEquals("dummdidumm.ids-mannheim.de", properties.getProperty("asset.host"));
        assertEquals("5", properties.getProperty("conf.page_size"));
        //Property file
        ExWSConf.clearProp();
        Properties propsec = ExWSConf.properties("exportPluginSec.conf");
        assertEquals("localhost", (propsec.getProperty("server.host")));
        assertEquals("1024", (propsec.getProperty("server.port")));
        assertEquals("korap.ids-mannheim.de", propsec.getProperty("api.host"));
        assertEquals("55", propsec.getProperty("conf.page_size"));
        assertEquals("ajlakjldkjf", propsec.getProperty("rtf.trail"));
    }

}

