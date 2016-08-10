package org.lep.senate.loader.document;

import org.jsoup.nodes.Document;

import java.io.FileNotFoundException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HeaderDocument {
    private static final String TYPE = "header";
    private static final Pattern TITLE_PATTERN =
            Pattern.compile("S.\\d+ - (.*) \\d+\\w+ Congress \\(\\d+-\\d+\\)");
    private static final Pattern SPONSOR_PATTERN =
            Pattern.compile("Sen. ([^,]+), ([\\w.]+)( [\\w.]+)?(,? \\w+.)?( \\(\\w+\\))? \\[(\\w)-(\\w\\w)\\]");

    private final String title;
    private final String[] sponsor;

    public HeaderDocument(int congressNum, int billNum) throws FileNotFoundException, ParseFieldException {
        Document doc = DocumentLoader.getBillDocument(congressNum, billNum, TYPE);
        title = parseTitle(doc);
        sponsor = parseSponsor(doc);
    }

    private static String parseTitle(Document doc) throws ParseFieldException {
        String headerText = doc.select(".legDetail").text();
        Matcher matcher = TITLE_PATTERN.matcher(headerText);
        if(!matcher.find()) {
            throw new ParseFieldException("Unable to parse title from \"" + headerText +
                    "\" using pattern \"" + TITLE_PATTERN.pattern() + "\"");
        }
        return matcher.group(1);
    }

    private static String[] parseSponsor(Document doc) throws ParseFieldException {
        String sponsorText = doc.select("tr:nth-child(1) a").text();
        Matcher matcher = SPONSOR_PATTERN.matcher(sponsorText);
        if(!matcher.find()) {
            throw new ParseFieldException("Unable to parse sponsor from \"" + sponsorText +
                    "\" using pattern \"" + SPONSOR_PATTERN.pattern() + "\"");
        }
        String last = matcher.group(1);
        String first = matcher.group(2);
//        String middle = matcher.group(3);
//        middle = middle == null ? "" : middle.trim();
//        String suffix = matcher.group(4);
//        suffix = suffix == null ? "" : suffix.substring(2);
//        String nickname = matcher.group(5);
//        String party = matcher.group(6);
        String state = matcher.group(7);
        return new String[]{first, last, state};
    }

    public String getTitle() { return title; }

    public String[] getSponsor() { return sponsor; }
}
