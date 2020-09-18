package de.ids_mannheim.korap.plkexport;

public class Util {
    public static String sanitizeFileName (String fname) {
        return fname
            .replaceAll("[^\\p{L}0-9\\(\\)\\-\\_]", "-")
            .replaceAll("--+", "-")
            .replaceAll("([\\(\\)\\_])-+", "$1")
            .replaceAll("-+([\\(\\)\\_])", "$1")
            .replaceFirst("^-+","")
            .replaceFirst("-+$","")
            ;
    }
}
