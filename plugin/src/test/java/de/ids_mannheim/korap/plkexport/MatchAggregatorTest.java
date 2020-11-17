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
        m.setFname("Beispiel");
        assertEquals(m.getFname(),"Beispiel");
        m.setFname("contains(<s name=\"okay\">,[orth='Test'])");
        assertEquals(m.getFname(),"contains(s-name-okay-orth-Test)");
        assertEquals(m.getMimeType(),"text/plain");
        assertEquals(m.getSuffix(),"txt");
    };
};
