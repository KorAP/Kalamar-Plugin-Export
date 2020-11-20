package de.ids_mannheim.korap.plkexport;

import java.io.IOException;
import java.io.*;

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

	public static String streamToString (InputStream in) {
        StringBuilder sb = new StringBuilder();

        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(in));

            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line + System.lineSeparator());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

		return sb.toString();
	}
}
