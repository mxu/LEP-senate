package org.lep.senate.loader.document;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

public class DocumentLoader {
    private static final String DATA_ROOT = "/Users/mike.xu/code/LEP-senate/data";
    private static final String CONGRESS_DIR_FORMAT = DATA_ROOT + "/%d";
    private static final String BILL_DIR_FORMAT = CONGRESS_DIR_FORMAT + "/%d";
    private static final String BILL_DOC_FORMAT = BILL_DIR_FORMAT + "/%s.html";

    public static File[] getCongressFolders() {
        return new File(DATA_ROOT).listFiles(File::isDirectory);
    }

    public static File[] getBillFolders(int congressNum) {
        String path = String.format(CONGRESS_DIR_FORMAT, congressNum);
        return new File(path).listFiles(File::isDirectory);
    }

    public static Document getBillDocument(int congressNum, int billNum, String type) throws FileNotFoundException {
        Document doc;

        String path = String.format(BILL_DOC_FORMAT, congressNum, billNum, type);
        File input = new File(path);

        try {
            doc = Jsoup.parse(input, "UTF-8", "https://www.congress.gov");
        } catch(IOException e) {
            throw new FileNotFoundException("Missing document \"" + input + "\"");
        }

        return doc;
    }
}
