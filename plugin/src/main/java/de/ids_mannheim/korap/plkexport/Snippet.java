package de.ids_mannheim.korap.plkexport;

public class Snippet {

    private String left;
    private String right;
    private String mark;


    public Snippet (String snippetstr) {
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
