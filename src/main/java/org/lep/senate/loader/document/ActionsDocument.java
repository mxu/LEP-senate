package org.lep.senate.loader.document;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.lep.senate.biz.MissingActionException;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

public class ActionsDocument {
    private final List<String> actions;

    public ActionsDocument(int congressNum, int billNum) throws FileNotFoundException, MissingActionException {
        Document doc = DocumentLoader.getBillDocument(congressNum, billNum, "all-actions");
        actions = parseActions(doc);
    }

    private static List<String> parseActions(Document doc) throws MissingActionException {
        List<String> result = new ArrayList<>();
        for(Element e : doc.select("td.actions")) {
            result.add(e.text());
        }

        if(result.size() == 0) {
            throw new MissingActionException("Bill must contain at least one action");
        }

        return result;
    }

    public List<String> getActions() { return actions; }
}
