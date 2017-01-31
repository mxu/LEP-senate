package org.lep.senate.loader.document;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.stream.Collectors;

public class DocumentLoader {
    private static final String DATA_ROOT = "/Users/mike.xu/code/LEP-senate/data";
    private static final String CONGRESS_DIR_FORMAT = DATA_ROOT + "/%d";
    private static final String BILL_DIR_FORMAT = CONGRESS_DIR_FORMAT + "/%d";
    private static final String BILL_DOC_FORMAT = BILL_DIR_FORMAT + "/%s.html";
    private static final String AMDT_DIR_FORMAT = CONGRESS_DIR_FORMAT + "/amdt";

    public static File[] getCongressFolders() {
        return new File(DATA_ROOT).listFiles(File::isDirectory);
    }

    public static File[] getBillFolders(int congressNum) {
        String path = String.format(CONGRESS_DIR_FORMAT, congressNum);
        return Arrays.stream(new File(path).listFiles(File::isDirectory))
                .filter(f -> !f.getName().equals("amdt"))
                .toArray(File[]::new);
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

    public static File[] getAmdtFiles(int congressNum) {
        String path = String.format(AMDT_DIR_FORMAT, congressNum);
        return new File(path).listFiles(File::isFile);
    }

    public static Document getAmendmentIndexDocument(File f) throws FileNotFoundException {
        Document doc;

        try {
            doc = Jsoup.parse(f, "UTF-8", "https://www.congress.gov");
        } catch(IOException e) {
            throw new FileNotFoundException("Missing document \"" + f + "\"");
        }

        return doc;
    }
}
