package de.ids_mannheim.korap.plkexport;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class Snippet {

    private String left, right, mark;
    private boolean leftMore, rightMore, cuttedMark;

    private static Pattern snippetP =
        Pattern.compile("^(?i)<span[^>]+class=\"(?:[^\"]* )?context-left(?:[^\"]* )?\">(.*?)</span>" +
                        "<span[^>]+class=\"(?:[^\"]* )?match(?:[^\"]* )?\">(.+?)</span>" +
                        "<span[^>]+class=\"(?:[^\"]* )?context-right(?:[^\"]* )?\">(.*?)</span>$");   

    private static Pattern moreP =
        Pattern.compile("(?i)<span[^>]+class=\"more\"></span>");

    private static Pattern cuttedP =
        Pattern.compile("(?i)<span[^>]+class=\"cutted\"></span>");
    
    public Snippet (String snippetstr) {

        // Match with precise algorithm
        String left, right;
        Matcher m = snippetP.matcher(snippetstr);
        if (m.find()) {
            left = m.group(1);
            mark = m.group(2);
            right = m.group(3);

            if (left != null) {
                m = moreP.matcher(left);
                if (m.find()) {
                    left = m.replaceAll("");
                    this.leftMore = true;
                };
                this.setLeft(unescapeHTML(left));
            };

            m = cuttedP.matcher(mark);
            if (m.find()) {
                mark = m.replaceAll("");
                this.cuttedMark = true;
            };
            
            this.setMark(unescapeHTML(mark.replaceAll("</?mark[^>]*>", "")));

            if (right != null) {
                m = moreP.matcher(right);
                if (m.find()) {
                    right = m.replaceAll("");
                    this.rightMore = true;
                };
                this.setRight(unescapeHTML(right));
            };
        }

        // Simpler mark-split algorithm
        else {
            String[] splitted = snippetstr
                .replaceAll("(?i)</?span[^>]*>","")
                .split("(?i)</?mark[^>]*>");
            if (splitted[0] != null) {
                this.setLeft(splitted[0]);
            };
            if (splitted[1] != null) {
                this.setMark(splitted[1]);
            };
            if (splitted[2] != null) {
                this.setRight(splitted[2]);
            };
            
            return;
        };
    }

    public String getLeft () {
        return left;
    }


    public void setLeft (String left) {
        this.left = left;
    }


    public String getRight () {
        return right;
    }


    public void setRight (String right) {
        this.right = right;
    }


    public String getMark () {
        return mark;
    }


    public void setMark (String mark) {
        this.mark = mark;
    }

    
    public boolean hasMoreLeft () {
        return leftMore;
    };


    public boolean hasMoreRight () {
        return rightMore;
    };

    public boolean isCutted () {
        return cuttedMark;
    };
    
    private static String unescapeHTML (String text) {
        if (text == null)
			return "";
		
        return text
            .replace("&quot;", "\"")
            .replace("&apos;", "'")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&amp;", "&");
    };
}
