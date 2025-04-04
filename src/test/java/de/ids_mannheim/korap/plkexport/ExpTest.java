package de.ids_mannheim.korap.plkexport;

import static org.junit.jupiter.api.Assertions.fail;

//Fixture loading
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URLDecoder;

import org.glassfish.jersey.test.JerseyTest;


public class ExpTest extends JerseyTest  {
    
    public String getFixture (String file) {
        return getFixture(file, true);
    }

     private  String getFixture (String file, Boolean raw) {
            String filepath = getClass()
            .getResource("/fixtures/" + file)
            .getFile();
        
        if (raw) {
            return getFileString(filepath);
        };
        return Util.convertToUTF8(getFileString(filepath));
    };
        
    // Get string from a file
    public static String getFileString (String filepath) {
        StringBuilder contentBuilder = new StringBuilder();
        try {
			BufferedReader in = new BufferedReader(
                new InputStreamReader(
                    new FileInputStream(URLDecoder.decode(filepath, "UTF-8")),
                    "UTF-8"
                    )
                );

            String str;
            while ((str = in.readLine()) != null) {
                contentBuilder.append(str);
            };
            in.close();
        }
        catch (IOException e) {
            fail(e.getMessage());
        }
        return contentBuilder.toString();
    };

}
