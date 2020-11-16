package de.ids_mannheim.korap.plkexport;

import java.util.regex.Pattern;

public class Snippet {

    private String left, right, mark;
    private boolean leftMore, rightMore;

    private static Pattern leftMoreP =
        Pattern.compile("(?i)<span[^>]*?class=\"more\".+<mark>");
    private static Pattern rightMoreP =
        Pattern.compile("(?i)</mark>.+<span[^>]*?class=\"more\"");

    public Snippet (String snippetstr) {

        // Check the context
        this.leftMore = this.rightMore = false;
        if (leftMoreP.matcher(snippetstr).find()) {
            this.leftMore = true;
        };
        if (rightMoreP.matcher(snippetstr).find()) {
            this.rightMore = true;
        };

        // Split the match
        String[] split = snippetstr
            .replaceAll("(?i)</?span[^>]*>", "")
            .split("</?mark>");

        this.setLeft(unescapeHTML(split[0].trim()));
        this.setMark(unescapeHTML(split[1].trim()));
        this.setRight(unescapeHTML(split[2].trim()));
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
