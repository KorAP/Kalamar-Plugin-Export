package de.ids_mannheim.korap.plkexport;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
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
};
