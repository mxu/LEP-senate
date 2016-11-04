package org.lep.senate.loader.document;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.lep.senate.biz.MissingActionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

public class ActionsDocument {
    private static final String HOUSE = "House";
    private static final String CHAMBER_SELECTOR = "td:nth-child(2)";
    private static final String ACTIONS_SELECTOR = "td.actions";
    private static final Logger logger = LoggerFactory.getLogger(ActionsDocument.class);

    private final List<String> actions;

    public ActionsDocument(int congressNum, int billNum) throws FileNotFoundException, MissingActionException {
        Document doc = DocumentLoader.getBillDocument(congressNum, billNum, "all-actions");

        actions = parseActions(doc);
    }

    private static List<String> parseActions(Document doc) throws MissingActionException {
        List<String> result = new ArrayList<>();
        List<Integer> senateActions = new ArrayList<>();

        int i = 0;
        for(Element e : doc.select(CHAMBER_SELECTOR)) {
            if(!e.text().equals(HOUSE)) {
               senateActions.add(i);
            }
            i++;
        }

        Elements actions = doc.select(ACTIONS_SELECTOR);
        for(Integer index : senateActions) {
            result.add(actions.get(index).text());
        }

        if(result.size() == 0) {
            throw new MissingActionException("Bill must contain at least one action");
        }

        return result;
    }

    public List<String> getActions() { return actions; }
}
