package de.ids_mannheim.korap.plkexport;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;
import org.junit.Test;

import de.ids_mannheim.korap.plkexport.MatchAggregator;

public class MatchAggregatorTest {

    @Test
    public void testEmptyInit () throws IOException {
        MatchAggregator m = new MatchAggregator();
        m.init("");
        assertNull(m.getMeta());
        assertNull(m.getQuery());
        assertNull(m.getCollection());

        m = new MatchAggregator();
        m.init(null);
        assertNull(m.getMeta());
        assertNull(m.getQuery());
        assertNull(m.getCollection());
    };

    @Test
    public void testSampleInit () throws IOException {
        MatchAggregator m = new MatchAggregator();
        m.init("{\"meta\":{\"totalResults\":6}}");
        assertEquals(m.getMeta().toString(),"{\"totalResults\":6}");
        assertNull(m.getQuery());
        assertNull(m.getCollection());
    };
  
    @Test
    public void testMatchesInit () throws IOException {
        MatchAggregator m = new MatchAggregator();
        m.init("{\"matches\":[\"first\",\"second\"]}");
        assertNull(m.getMeta());
        assertNull(m.getQuery());
        assertNull(m.getCollection());
    };

    @Test
    public void testAttributes () throws IOException {
        MatchAggregator m = new MatchAggregator();
        m.setFileName("Beispiel");
        assertEquals(m.getFileName(),"Beispiel");
        m.setFileName("contains(<s name=\"okay\">,[orth='Test'])");
        assertEquals(m.getFileName(),"contains(s-name-okay-orth-Test)");
        assertEquals(m.getMimeType(),"text/plain");
        assertEquals(m.getSuffix(),"txt");
    };

    @Test
    public void testStrings () throws IOException {
        MatchAggregator m = new MatchAggregator();
        m.setQueryString("Beispiel-Query");
        assertEquals(m.getQueryString(),"Beispiel-Query");

        m.setCorpusQueryString("Beispiel-Corpus");
        assertEquals(m.getCorpusQueryString(),"Beispiel-Corpus");
    };

    @Test
    public void testTimeout () throws IOException {
        MatchAggregator m = new MatchAggregator();
        m.init("{\"meta\":{\"totalResults\":6}}");
        assertEquals(m.getTotalResults(),6);
        assertFalse(m.hasTimeExceeded());

        m = new MatchAggregator();
        m.init("{\"meta\":{\"totalResults\":7,\"timeExceeded\":true}}");
        assertEquals(m.getTotalResults(),7);
        assertTrue(m.hasTimeExceeded());

        m = new MatchAggregator();
        m.init("{\"meta\":{\"totalResults\":8,\"timeExceeded\":false}}");
        assertEquals(m.getTotalResults(),8);
        assertFalse(m.hasTimeExceeded());
    };
    
    @Test
    public void testFileName () throws IOException {
        MatchAggregator m = new MatchAggregator();
        assertEquals(m.getFileName(),"export");
        
        m = new MatchAggregator();
        m.setFileName("Beispiel");
        assertEquals(m.getFileName(),"Beispiel");

        m = new MatchAggregator();
        m.setQueryString("contains(<s name=\"okay\">,[orth='Test'])");
        assertEquals(m.getQueryString(),"contains(<s name=\"okay\">,[orth='Test'])");
        assertEquals(m.getFileName(),"contains(s-name-okay-orth-Test)");
        m.setFileName("Beispiel");
        assertEquals(m.getFileName(),"Beispiel");
    };
};
