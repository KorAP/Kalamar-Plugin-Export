package de.ids_mannheim.korap.plkexport;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Representation of a match.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class MatchExport {

    private String textSigle,
        author,
        pubDate,
        snippet,
        title;

    private Snippet snippeto;

    /**
     * Get author of the match.
     */
    public String getAuthor () {
        return author;
    };


    /**
     * Get textSigle of the match.
     */
    public String getTextSigle () {
        return textSigle;
    };

    
    /**
     * Get title of the match.
     */
    public String getTitle () {
        return title;
    };

    
    /**
     * Get publication date of the match.
     */
    public String getPubDate () {
        return pubDate;
    };

    
    /**
     * Get snippet object of the match.
     */
    public Snippet getSnippet () {
        return snippeto;
    };
    

    /**
     * Get snippet string of the match.
     */
    public String getSnippetString () {
        return snippet;
    };

    
    /*
     * Override setter object.
     */
    private void setSnippet (String snippet) {
        this.snippet = snippet;
        this.snippeto = new Snippet(this.snippet);
    };
};
