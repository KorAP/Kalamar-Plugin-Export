package de.ids_mannheim.korap.plkexport;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;

public class MatchTest {

    private static final ObjectMapper mapper = new ObjectMapper();

    @Test
    public void testSimple () throws JsonProcessingException, IOException {

        String match = "{\"author\":\"Goethe\","+
        "\"title\":\"Title1\","+
        "\"pubDate\":\"20051103\","+
        "\"textSigle\":\"RTF/G59/34284\","+
        "\"snippet\":\"<span class=\\\"context-left\\\"></span><span class=\\\"match\\\"><mark>Und dafür, dass</mark><span class=\\\"cutted\\\"></span></span><span class=\\\"context-right\\\"> meine IP öffentlich angezeigt wird. Über die IP kann man auf den Wohnort, den Provider und bei Aufenthalt am Arbeitsplatz auf den Arbeitgeber schließen, über Konto nicht. -- 09:24, 17. Dez. 2011 (CET) Bist Du denn nicht mehr selber Arbeitgeber? -- 09:31<span class=\\\"more\\\"></span></span>\"}";

        Match matchObj = mapper.readValue(match, Match.class);

        assertEquals(matchObj.getAuthor(), "Goethe");
        assertEquals(matchObj.getTitle(), "Title1");
        assertEquals(matchObj.getPubDate(), "20051103");
        assertEquals(matchObj.getTextSigle(), "RTF/G59/34284");
        
        assertTrue(matchObj.getSnippetString().contains("<span class"));

        assertEquals(matchObj.getSnippet().getMark(),
                     "Und dafür, dass");
        
    };
    
};
