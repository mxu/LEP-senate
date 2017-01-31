package org.lep.senate.loader.document;

import javafx.util.Pair;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AmendmentIndexDocument {
    private static final Logger logger = LoggerFactory.getLogger(AmendmentIndexDocument.class);

    private static final String ENTRY = ".expanded";
    private static final String AMDT_LINK = ".result-heading a:first-child";
    private static final String SPONSOR_LINK = ".result-item:contains(Sponsor) a";
    private static final String BILL_LINK = ".result-item:contains(Amends Bill) a";

    private static final Pattern AMDT_PATTERN =
            Pattern.compile("S(\\.Up)?\\.Amdt\\.(\\d+)");
    private static final Pattern SPONSOR_PATTERN =
            Pattern.compile("Sen. ([^,]+), ([\\w.]+)( [\\w.]+)?(,? \\w+.)?( \\(\\w+\\))? \\[(\\w+)-(\\w+)\\]");
    private static final Pattern BILL_PATTERN =
            Pattern.compile("S.(\\d+)");

    private final Map<String, Pair<Integer, String[]>> amdtBillSponsors;
    // amdtNum -> billNum, sponsorInfo[]

    public AmendmentIndexDocument(File f) throws FileNotFoundException, ParseFieldException {
        Document doc = DocumentLoader.getAmendmentIndexDocument(f);

        amdtBillSponsors = parseAmdtIndex(doc);
    }

    private static Map<String, Pair<Integer, String[]>> parseAmdtIndex(Document doc) throws ParseFieldException {
        Map<String, Pair<Integer, String[]>> result = new HashMap<>();

        for(Element e : doc.select(ENTRY)) {
            Integer billNum;
            // skip amendments that don't link to a senate bill
            try {
                billNum = parseBillNum(e);
            } catch(ParseFieldException exception) {
                continue;
            }

            try {
                String amdtNum = parseAmdtNum(e);
                String[] sponsor = parseSponsor(e);
                result.put(amdtNum, new Pair<>(billNum, sponsor));
            } catch(ParseFieldException exc) {
                logger.error("Failed to process entry: {}", exc);
            }
        }

        return result;
    }

    private static String parseAmdtNum(Element e) throws ParseFieldException {
        String amdtText = e.select(AMDT_LINK).text();
        String amdtNum = getAmdtNum(amdtText);
        if(amdtNum == null) {
            throw new ParseFieldException("Unable to parse amendment number from \"" + amdtText +
                    "\" using pattern \"" + AMDT_PATTERN.pattern() + "\"");
        }

        return amdtNum;
    }

    public static String getAmdtNum(String amdtText) {
        Matcher matcher = AMDT_PATTERN.matcher(amdtText);
        if(!matcher.find()) {
            return null;
        }

        String amdtNum = matcher.group(2);
        if(matcher.group(1) != null) {
            amdtNum = "U" + amdtNum;
        }
        return amdtNum;
    }

    private static String[] parseSponsor(Element e) throws ParseFieldException {
        String sponsorText = e.select(SPONSOR_LINK).text();
        Matcher matcher = SPONSOR_PATTERN.matcher(sponsorText);
        if(!matcher.find()) {
            throw new ParseFieldException("Unable to parse sponsor from \"" + sponsorText +
                    "\" using pattern \"" + SPONSOR_PATTERN.pattern() + "\"");
        }
        String last = matcher.group(1);
        String first = matcher.group(2);
        String state = matcher.group(7);
        return new String[]{first, last, state};
    }

    private static Integer parseBillNum(Element e) throws ParseFieldException {
        String billText = e.select(BILL_LINK).text();
        Matcher matcher = BILL_PATTERN.matcher(billText);
        if(!matcher.find()) {
            throw new ParseFieldException("Unable to parse bill number from \"" + billText +
                    "\" using pattern \"" + BILL_PATTERN.pattern() + "\"");
        }

        return Integer.parseInt(matcher.group(1));
    }

    public Map<String, Pair<Integer, String[]>> getAmdtBillSponsors() {
        return amdtBillSponsors;
    }
}
