package de.ids_mannheim.korap.plkexport;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.Test;

import static de.ids_mannheim.korap.plkexport.Util.*;

public class UtilTest {

    @Test
    public void testSanitizeFileName () {
        assertEquals(sanitizeFileName("[orth='Test']"), "orth-Test");
        assertEquals(sanitizeFileName("contains(<s name=\"okay\">,[orth='Test'])"), "contains(s-name-okay-orth-Test)");
    };
};
