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
        MatchAggregator m = new MatchAggregator("");
        assertNull(m.meta);
        assertNull(m.query);
        assertNull(m.collection);

        MatchAggregator.MatchIterator mi = m.iterator();

        assertFalse(mi.hasNext());
        assertNull(mi.next());

        m = new MatchAggregator(null);
        assertNull(m.meta);
        assertNull(m.query);
        assertNull(m.collection);
    };

    @Test
    public void testSampleInit () throws IOException {
        MatchAggregator m = new MatchAggregator(
            "{\"meta\":{\"totalResults\":6}}"
            );
        assertEquals(m.meta.toString(),"{\"totalResults\":6}");
        assertNull(m.query);
        assertNull(m.collection);
    };
  
    @Test
    public void testMatchesInit () throws IOException {
        MatchAggregator m = new MatchAggregator(
            "{\"matches\":[\"first\",\"second\"]}"
            );
        assertNull(m.meta);
        assertNull(m.query);
        assertNull(m.collection);

        MatchAggregator.MatchIterator mi = m.iterator();

        assertTrue(mi.hasNext());
        assertEquals(mi.next().toString(),"\"first\"");
        assertTrue(mi.hasNext());
        assertEquals(mi.next().toString(),"\"second\"");
        assertFalse(mi.hasNext());
    };
};
