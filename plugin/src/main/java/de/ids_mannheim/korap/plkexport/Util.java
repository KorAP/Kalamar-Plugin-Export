package de.ids_mannheim.korap.plkexport;

import java.io.IOException;
import java.io.*;

/**
 * Utility class to provide helper functions.
 */
public class Util {

    /**
     * Sanitize a file name to not containe invalid characters.
     */
    public static String sanitizeFileName (String fname) {
        return fname
            .replaceAll("[^\\p{L}0-9\\(\\)\\-\\_]", "-")
            .replaceAll("--+", "-")
            .replaceAll("([\\(\\)\\_])-+", "$1")
            .replaceAll("-+([\\(\\)\\_])", "$1")
            .replaceFirst("^-+","")
            .replaceFirst("-+$","")
            ;
    };

    /**
     * Create a string representation of an inputstream.
     */
	public static String streamToString (InputStream in) {
        StringBuilder sb = new StringBuilder();

        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(in));

            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line + System.lineSeparator());
            }
        }

        catch (IOException e) {
            e.printStackTrace();
        }

		return sb.toString();
	};

    /**
     * Convert string to utf8
     */
    public static String convertToUTF8(String s) {
        if (s == null)
            return null;
        String out = null;
        try {
            out = new String(s.getBytes("UTF-8"), "ISO-8859-1");
        } catch (java.io.UnsupportedEncodingException e) {
            return null;
        }
        return out;
    };

    /**
     * Convert string from utf8
     */
    public static String convertFromUTF8(String s) {
        if (s == null)
            return null;
        String out = null;
        try {
            out = new String(s.getBytes("Windows-1252"), "UTF-8");
        } catch (java.io.UnsupportedEncodingException e) {
            return null;
        }
        return out;
    };
};
