package org.lep.senate.loader.document;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.lep.senate.biz.MissingActionException;

import java.io.FileNotFoundException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HeaderDocument {
    private static final String TYPE = "header";
    private static final Pattern TITLE_PATTERN =
            Pattern.compile("S.\\d+ - (.*) \\d+\\w+ Congress \\(\\d+-\\d+\\)");
    private static final Pattern SPONSOR_PATTERN =
            Pattern.compile("Sen. ([^,]+), ([\\w.]+)( [\\w.]+)?(,? \\w+.)?( \\(\\w+\\))? \\[(\\w+)-(\\w+)\\]");

    private final String title;
    private final String[] sponsor;
    private final String latestAction;

    public HeaderDocument(int congressNum, int billNum, boolean getLatestAction)
            throws FileNotFoundException, ParseFieldException, MissingActionException {
        Document doc = DocumentLoader.getBillDocument(congressNum, billNum, TYPE);

        title = parseTitle(doc);

        // NOTE(mike.xu): temporary fix for incorrect data from congress.gov
        if(congressNum == 108 && billNum == 1501) {
            sponsor = new String[] {"John", "McCain", "AZ"};
        } else {
            sponsor = parseSponsor(doc);
        }

        // NOTE(mike.xu): fall back to latest action if actions tab is empty
        latestAction = getLatestAction ? parseLatestAction(doc) : null;
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

    private static String parseLatestAction(Document doc) throws MissingActionException {
        Element e = doc.select("tr:nth-child(2) td").get(0);
        String latestAction = e.text();
        if(latestAction != null && !latestAction.equals("")) {
            return latestAction;
        } else {
            throw new MissingActionException("Could not find latest action in header");
        }
    }

    public String getTitle() { return title; }

    public String[] getSponsor() { return sponsor; }

    public String getLatestAction() { return latestAction; }
}
