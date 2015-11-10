package testpurpose;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.StringTokenizer;

/**
 * Created by jufey on 10.11.15.
 */
public class test1readFile {
    public static void main(String[] args) {

        //HashMap hm = loadMimeTypes("/home/jufey/Dropbox/Uni/Rechnernetze/Praktikum/praktisch01_src/mime.types");

        //System.out.println(hm.get("png"));
        //System.out.println(hm.get("mpeg"));
        //System.out.println(hm.get("mpg"));
        String fileName ="/home/jufey/Dropbox/Uni/Rechnernetze/Pr.aktikum/pra.ktisch01_src/mime.types";
        String type = fileName.substring(fileName.lastIndexOf('.')+1);
        System.out.println(type);


    }

    private static HashMap loadMimeTypes(String mimeFilePath) {
        HashMap mimetype = new HashMap();
        String key;
        String value;

        //Check if file is present
        File file = new File(mimeFilePath);
        if (!file.canRead() || !file.isFile()) {
            System.exit(0);
        }

        StringTokenizer tokens;
        int tokencount;
        BufferedReader in = null;
        try {
            in = new BufferedReader(new FileReader(mimeFilePath));
            String zeile = null;
            while ((zeile = in.readLine()) != null) {
                if (!zeile.startsWith("#")) {
                    tokens = new StringTokenizer(zeile);
                    tokencount = tokens.countTokens();
                    if (tokencount < 2) {
                        continue;
                    }
                    value = tokens.nextToken();
                    while (tokens.hasMoreElements()) {
                        key = tokens.nextElement().toString();
                        mimetype.put(key, value);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (in != null)
                try {
                    in.close();
                } catch (IOException e) {
                }
        }
        return mimetype;
    }


}
