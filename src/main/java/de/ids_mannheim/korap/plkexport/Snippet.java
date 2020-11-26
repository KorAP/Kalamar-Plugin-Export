package de.ids_mannheim.korap.plkexport;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * Representation of a match snippet.
 */
public class Snippet {

    private String left, right, mark;
    private boolean leftMore, rightMore, cuttedMark;

    // Pattern to get Snippet match and contexts
    private static Pattern snippetP =
        Pattern.compile("^(?i)<span[^>]+class=\"(?:[^\"]* )?context-left(?:[^\"]* )?\">(.*?)</span>" +
                        "<span[^>]+class=\"(?:[^\"]* )?match(?:[^\"]* )?\">(.+?)</span>" +
                        "<span[^>]+class=\"(?:[^\"]* )?context-right(?:[^\"]* )?\">(.*?)</span>$");   

    // Pattern to check if more context is available
    private static Pattern moreP =
        Pattern.compile("(?i)<span[^>]+class=\"more\"></span>");

    // Pattern to check if the match is actually larger, but was cutted down
    private static Pattern cuttedP =
        Pattern.compile("(?i)<span[^>]+class=\"cutted\"></span>");


    /**
     * Constructor for Snippet parsing
     */
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
                this.left = unescapeHTML(left);
            };

            m = cuttedP.matcher(mark);
            if (m.find()) {
                mark = m.replaceAll("");
                this.cuttedMark = true;
            };
            
            this.mark = unescapeHTML(mark.replaceAll("</?mark[^>]*>", ""));

            if (right != null) {
                m = moreP.matcher(right);
                if (m.find()) {
                    right = m.replaceAll("");
                    this.rightMore = true;
                };
                this.right = unescapeHTML(right);
            };
        }

        // Simpler mark-split algorithm, mainly used for testing
        else {
            String[] splitted = snippetstr
                .replaceAll("(?i)</?span[^>]*>","")
                .split("(?i)</?mark[^>]*>");
            if (splitted[0] != null) {
                this.left = splitted[0];
            };
            if (splitted[1] != null) {
                this.mark = splitted[1];
            };
            if (splitted[2] != null) {
                this.right = splitted[2];
            };
            
            return;
        };
    };


    /**
     * Get the left context
     */
    public String getLeft () {
        return left;
    };


    /**
     * Get the right context.
     */
    public String getRight () {
        return right;
    };


    /**
     * Get the marked match.
     */
    public String getMark () {
        return mark;
    };


    /**
     * Get information if there is more context to the left.
     */
    public boolean hasMoreLeft () {
        return leftMore;
    };


    /**
     * Get information if there is more context to the right.
     */
    public boolean hasMoreRight () {
        return rightMore;
    };

    /**
     * Get information if the match was cutted.
     */
    public boolean isCutted () {
        return cuttedMark;
    };


    /*
     * Unescape HTML entities.
     */
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
};
