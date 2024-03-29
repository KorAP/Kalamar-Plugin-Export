package de.ids_mannheim.korap.plkexport;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import org.junit.Test;

public class SnippetTest {

    @Test
    public void testSimple () {
        Snippet s = new Snippet("<span class=\"context-left\">Der </span><span class=\"match\"><mark>Plagegeist</mark></span><span class=\"context-right\"> ging um</span>");
        assertEquals(s.getLeft(),"Der ");
        assertEquals(s.getRight()," ging um");
        assertEquals(s.getMark(),"Plagegeist");
        assertFalse(s.hasMoreLeft());
        assertFalse(s.hasMoreRight());
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
        assertTrue(s.hasMoreLeft());
        assertTrue(s.hasMoreRight());
    };

    @Test
    public void testMultipleMarks () {
        Snippet s = new Snippet("<span class=\"context-left\"><span class=\"more\"></span>Figueras (gegen 1030, Kopialbuch der Abtei von Saint-Pé-de-Bigorre),  Figeres (1154, laut Pierre de Marcas Buch Histoire de Béarn),  Figueres (1421, Urkunden der Vicomté von Béarn),  Higueres (1750, Karte von Cassini),  Higueres (1793, Notice Communale) und   Higueres und Higuères (1801, Bulletin </span><span class=\"match\"><mark><mark class=\"class-2 level-0\">des <mark class=\"class-1 level-1\">lois</mark></mark><mark class=\"class-1 level-1\">). Toponyme</mark></mark></span><span class=\"context-right\"> und Erwähnungen von Souye waren:  Soyge und Soya (1538 bzw. 1547, Manuskriptsammlung des 16. bis 18. Jahrhunderts),  Souia (1645, Volkszählung von Morlaàs),  Souge und Souie (1675 bzw. 1682, Manuskriptsammlung des 16. bis 18. Jahrhunderts),  Souge (1750, Karte von Cassini),  Souye<span class=\"more\"></span></span>");
        assertEquals(s.getLeft(), "Figueras (gegen 1030, Kopialbuch der Abtei von Saint-Pé-de-Bigorre),  Figeres (1154, laut Pierre de Marcas Buch Histoire de Béarn),  Figueres (1421, Urkunden der Vicomté von Béarn),  Higueres (1750, Karte von Cassini),  Higueres (1793, Notice Communale) und   Higueres und Higuères (1801, Bulletin ");
        assertEquals(s.getRight()," und Erwähnungen von Souye waren:  Soyge und Soya (1538 bzw. 1547, Manuskriptsammlung des 16. bis 18. Jahrhunderts),  Souia (1645, Volkszählung von Morlaàs),  Souge und Souie (1675 bzw. 1682, Manuskriptsammlung des 16. bis 18. Jahrhunderts),  Souge (1750, Karte von Cassini),  Souye");
        assertEquals(s.getMark(),"des lois). Toponyme");
        assertTrue(s.hasMoreLeft());
        assertTrue(s.hasMoreRight());
    };

    @Test
    public void testCuttedAndEmptyContext () {
        Snippet s = new Snippet("<span class=\"context-left\"></span><span class=\"match\"><mark>Und dafür musstest Du extra ne neue Socke erstellen? Wieso traust Du Dich nicht, mit Deinem Account aufzutreten? - -- ωωσσI -  talk with me 09:17, 17. Dez. 2011 (CET) Der ist doch gesperrt. -- 09:21, 17. Dez. 2011 (CET) WWSS1, weil ich normalerweise mit IP schreibe und in dem Fall nicht möchte, dass</mark><span class=\"cutted\"></span></span><span class=\"context-right\"> meine IP öffentlich angezeigt wird. Über die IP kann man auf den Wohnort, den Provider und bei Aufenthalt am Arbeitsplatz auf den Arbeitgeber schließen, über Konto nicht. -- 09:24, 17. Dez. 2011 (CET) Bist Du denn nicht mehr selber Arbeitgeber? -- 09:31<span class=\"more\"></span></span>");
        assertEquals(s.getLeft(), "");
        assertEquals(s.getRight()," meine IP öffentlich angezeigt wird. Über die IP kann man auf den Wohnort, den Provider und bei Aufenthalt am Arbeitsplatz auf den Arbeitgeber schließen, über Konto nicht. -- 09:24, 17. Dez. 2011 (CET) Bist Du denn nicht mehr selber Arbeitgeber? -- 09:31");
        assertEquals(s.getMark(),"Und dafür musstest Du extra ne neue Socke erstellen? Wieso traust Du Dich nicht, mit Deinem Account aufzutreten? - -- ωωσσI -  talk with me 09:17, 17. Dez. 2011 (CET) Der ist doch gesperrt. -- 09:21, 17. Dez. 2011 (CET) WWSS1, weil ich normalerweise mit IP schreibe und in dem Fall nicht möchte, dass");
        assertTrue(s.isCutted());
        assertFalse(s.hasMoreLeft());
        assertTrue(s.hasMoreRight());
        
    };
};
