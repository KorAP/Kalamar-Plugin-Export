package de.ids_mannheim.korap.plkexport;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;
import org.junit.Test;

import de.ids_mannheim.korap.plkexport.Snippet;

public class SnippetTest {

    @Test
    public void testSimple () {
        Snippet s = new Snippet("<span class=\"context-left\">Der </span><span class=\"match\"><mark>Plagegeist</mark></span><span class=\"context-right\"> ging um</span>");
        assertEquals(s.getLeft(),"Der");
        assertEquals(s.getRight(),"ging um");
        assertEquals(s.getMark(),"Plagegeist");
    };
};
