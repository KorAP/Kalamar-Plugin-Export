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

    @Test
    public void testReal () {
        Snippet s = new Snippet("<span class=\"context-left\"><span class=\"more\"></span>und wie ich in einem Buch von Bernhard Karlgren gelesen habe, wird da eine alte Bedeutung &quot;Blutegel&quot; für dieses Zeichen angenommen, bzw. auch andere Ungeziefer konnten wohl gemeint sein. Der ma-Teil des Worts wurde also ursprünglich wahrscheinlich im Sinne von &quot;</span><span class=\"match\"><mark>Plagegeist</mark></span><span class=\"context-right\">&quot; verwendet, folglich war 蚂蚁 ursprünglich frei übersetzt eine &quot;Sch...-Ameise&quot; ;-) -- 18:21, 30. Apr. 2007 (CEST) Hallo Allgaeuer, mag sein, dass es für dich ein Hammer ist, aber es ist Stand der aktuellen Forschung in der Sinologie. Schriften von Karlgren u.<span class=\"more\"></span></span>");
        assertEquals(s.getLeft(),
                     "und wie ich in einem Buch von Bernhard Karlgren "+
                     "gelesen habe, wird da eine alte Bedeutung "+
                     "\"Blutegel\" "+
                     "für dieses Zeichen angenommen, bzw. auch andere "+
                     "Ungeziefer konnten wohl gemeint sein. Der ma-Teil "+
                     "des Worts wurde also ursprünglich wahrscheinlich "+
                     "im Sinne von \"");
        assertEquals(s.getRight(),"\" verwendet, folglich war 蚂蚁 ursprünglich frei übersetzt eine \"Sch...-Ameise\" ;-) -- 18:21, 30. Apr. 2007 (CEST) Hallo Allgaeuer, mag sein, dass es für dich ein Hammer ist, aber es ist Stand der aktuellen Forschung in der Sinologie. Schriften von Karlgren u.");
        assertEquals(s.getMark(),"Plagegeist");
    };
};
