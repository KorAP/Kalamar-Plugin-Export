package de.ids_mannheim.korap.plkexport;

public class Snippet {

    private String left;
    private String right;
    private String mark;


    public Snippet (String snippetstr) {
        String[] split = snippetstr.split("</?mark>");
        String splitleft = split[0];
        String splitmatch = split[1];
        String splitright = split[2];
        //(?i) makes the regex case insensitive.
        String splitleftr = splitleft.replaceAll("(?i)</?span[^>]*>", "");
        this.setLeft(splitleftr);
        String splitmatchr = splitmatch.replaceAll("(?i)</?span[^>]*>", "");
        this.setMark(splitmatchr);
        String splitrightr = splitright.replaceAll("(?i)</?span[^>]*>", "");
        this.setRight(splitrightr);
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
}
