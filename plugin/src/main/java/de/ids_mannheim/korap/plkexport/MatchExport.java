package de.ids_mannheim.korap.plkexport;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MatchExport {

    private String textSigle;
    private String author;
    private String pubDate;
    private Snippet snippeto;
    private String snippet;
    private String title;


    public String getAuthor () {
        return author;
    }


    public void setAuthor (String author) {
        this.author = author;
    }


    public String getTextSigle () {
        return textSigle;
    }


    public void setTextSigle (String textSigle) {
        this.textSigle = textSigle;
    }


    public Snippet getSnippetO () {
        return snippeto;
    }


    public void setSnippetO (Snippet snippet) {
        this.snippeto = snippet;
    }


    public String getSnippet () {
        return snippet;
    }


    public void setSnippet (String snippet) {
        this.snippet = snippet;
        this.snippeto = new Snippet(this.snippet);
    }


    public String getTitle () {
        return title;
    }


    public void setTitle (String title) {
        this.title = title;
    }


    public String getPubDate () {
        return pubDate;
    }


    public void setPubDate (String pubDate) {
        this.pubDate = pubDate;
    }
}
