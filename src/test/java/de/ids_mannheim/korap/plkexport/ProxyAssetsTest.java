package de.ids_mannheim.korap.plkexport;

import java.net.URI;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.MediaType;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.junit.Test;
import static org.junit.Assert.*;

public class ProxyAssetsTest extends JerseyTest {
    @Override
    protected Application configure() {
        return new ResourceConfig(Service.class);
    }

    @Test
    public void proxiedAssetsFollowFrontendAddress() {
        String page = target("export").request(MediaType.TEXT_HTML)
            .header("X-Proxy", "Kalamar").get(String.class);
        String script = "../../js/korap-plugin-latest.js";
        String style = "../../css/kalamar-plugin-latest.css";
        assertTrue(page.contains("src=\"" + script + "\""));
        assertTrue(page.contains("href=\"" + style + "\""));
        assertFalse(page.contains("https://dummdidumm.ids-mannheim.de/"));

        for (String frontend : new String[] {
                "http://localhost:64543/", "http://localhost:3543/",
                "https://korap.ids-mannheim.de/instance/ratskorpus/"}) {
            URI frame = URI.create(frontend + "plugin/export/export");
            assertEquals(frontend + "js/korap-plugin-latest.js", frame.resolve(script).toString());
            assertEquals(frontend + "css/kalamar-plugin-latest.css", frame.resolve(style).toString());
        }
    }

    @Test
    public void standaloneAssetsKeepConfiguredAddress() {
        String page = target("export").request(MediaType.TEXT_HTML).get(String.class);
        assertTrue(page.contains("https://dummdidumm.ids-mannheim.de/js/korap-plugin-latest.js"));
        assertTrue(page.contains("https://dummdidumm.ids-mannheim.de/css/kalamar-plugin-latest.css"));
    }
}
