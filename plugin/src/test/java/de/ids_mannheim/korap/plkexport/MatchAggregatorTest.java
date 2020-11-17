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
        assertNull(m.meta);
        assertNull(m.query);
        assertNull(m.collection);

        m = new MatchAggregator();
        m.init(null);
        assertNull(m.meta);
        assertNull(m.query);
        assertNull(m.collection);
    };

    @Test
    public void testSampleInit () throws IOException {
        MatchAggregator m = new MatchAggregator();
        m.init("{\"meta\":{\"totalResults\":6}}");
        assertEquals(m.meta.toString(),"{\"totalResults\":6}");
        assertNull(m.query);
        assertNull(m.collection);
    };
  
    @Test
    public void testMatchesInit () throws IOException {
        MatchAggregator m = new MatchAggregator();
        m.init("{\"matches\":[\"first\",\"second\"]}");
        assertNull(m.meta);
        assertNull(m.query);
        assertNull(m.collection);
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
