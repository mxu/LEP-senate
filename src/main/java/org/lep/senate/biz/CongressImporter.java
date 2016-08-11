package org.lep.senate.biz;

import com.beust.jcommander.JCommander;
import org.lep.senate.dao.SenateDAO;
import org.lep.senate.loader.document.ActionsDocument;
import org.lep.senate.loader.document.DocumentLoader;
import org.lep.senate.loader.document.HeaderDocument;
import org.lep.senate.loader.document.ParseFieldException;
import org.lep.senate.loader.resource.ImportantBillsList;
import org.lep.senate.loader.resource.RankingPhraseList;
import org.lep.senate.loader.resource.StepRegex;
import org.lep.senate.model.Congress;
import org.lep.senate.model.Senator;
import org.lep.senate.model.Step;
import org.lep.settings.CongressSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CongressImporter {
    private static final Logger logger = LoggerFactory.getLogger(CongressImporter.class);

    private static final SenateDAO dao = new SenateDAO();

    public static void main(String[] args) {
        CongressSettings settings = new CongressSettings();
        new JCommander(settings, args);

        Integer congressNum = settings.getCongress();
        Integer billNum = settings.getBill();

        dao.resetBills();
        if(congressNum == null) {
            processCongresses();
        } else {
            if(billNum == null) {
                processCongress(congressNum);
            } else {
                try {
                    processBill(congressNum, billNum);
                } catch (Exception e) {
                    logger.error("Failed to process bill {} ({}): {}", billNum, congressNum, e.getMessage());
                    e.printStackTrace();
                }
            }
        }
    }

    private static void processCongresses() {
        try {
            for(Step step : Step.values()) {
                StepRegex.getRegexList(step);
            }
            RankingPhraseList.getRankingPhrases();
            RankingPhraseList.getDerankingPhrases();
        } catch (FileNotFoundException e) {
            logger.error("Failed to process congresses: {}", e.getMessage());
            return;
        }

        List<Integer> congressNums = Arrays.stream(DocumentLoader.getCongressFolders())
                .map(folder -> Integer.parseInt(folder.getName()))
                .sorted()
                .collect(Collectors.toList());

        for(Integer congressNum : congressNums) {
            processCongress(congressNum);
        }
    }

    private static void processCongress(int congressNum) {
        logger.info("Processing congress {}...", congressNum);

        try {
            for(Step step : Step.values()) {
                StepRegex.getRegexList(step);
            }
            RankingPhraseList.getRankingPhrases();
            RankingPhraseList.getDerankingPhrases();
            ImportantBillsList.getImportantBills(congressNum);
        } catch (FileNotFoundException e) {
            logger.error("Failed to process congress {}: {}", congressNum, e.getMessage());
            return;
        }

        Congress c = dao.getOrAddCongress(congressNum);

        int billsProcessed = 0;
        int billsFailed = 0;

        long startTime = System.nanoTime();

        List<Integer> billNums = Arrays.stream(DocumentLoader.getBillFolders(c.getId()))
                .map(folder -> Integer.parseInt(folder.getName()))
                .sorted()
                .collect(Collectors.toList());

        for(Integer billNum : billNums) {
           try {
               processBill(c.getId(), billNum);
               billsProcessed++;
           } catch(Exception e) {
               logger.error("Failed to process bill {} ({}): {}", billNum, congressNum, e.getMessage());
               billsFailed++;
           }
        }
        long endTime = System.nanoTime();
        long totalTime = (endTime - startTime) / 1000000000;

        logger.info("Finished processing {}/{} bills in {}s", billsProcessed, billsFailed, totalTime);
    }

    private static void processBill(int congressNum, int billNum)
            throws FileNotFoundException, ParseFieldException, MissingSenatorException, MissingActionException {
        Map<Step, Boolean> stepsMatched = createStepsMap();
        List<String> actions = null;
        boolean getLatestAction = false;
        try {
            ActionsDocument actionDoc = new ActionsDocument(congressNum, billNum);
            actions = actionDoc.getActions();
        } catch (MissingActionException e) {
            getLatestAction = true;
        }

        HeaderDocument headerDoc = new HeaderDocument(congressNum, billNum, getLatestAction);
        String title = headerDoc.getTitle();

        String[] sponsor = headerDoc.getSponsor();
        Senator s = dao.getSenator(sponsor);
        if(s == null) {
            throw new MissingSenatorException(String.format("Unable to find senator (%s) %s %s",
                    sponsor[2], sponsor[0], sponsor[1]));
        }

        int importance = getImportance(congressNum, billNum, title);

        if(getLatestAction) {
            actions = Collections.singletonList(headerDoc.getLatestAction());
            logger.warn("Missing actions for bill {} ({}), falling back to latest action from header", billNum, congressNum);
        }

        for(String action : actions) {
            for(Step step : stepsMatched.keySet()) {
                if(!stepsMatched.get(step)) {
                    stepsMatched.put(step, StepRegex.matchesStepRegex(step, action));
                }
            }
        }

        if(!stepsMatched.get(Step.BILL)) {
            logger.debug("blah");
        }

        dao.createBill(congressNum, billNum, title, importance, s.getId(), stepsMatched);
    }

    private static int getImportance(int congressNum, int billNum, String title) throws FileNotFoundException {
        int importance = 2;

        logger.debug("Get importance for bill {} ({}) \"{}\"", billNum, congressNum, title);
        List<Integer> importantBills = ImportantBillsList.getImportantBills(congressNum);
        if(importantBills.contains(billNum)) {
            logger.debug("Found in important list");
            importance = 3;
        } else if(RankingPhraseList.containsRankingPhrase(title)) {
            logger.debug("Found matching ranking phrase");
            importance = 1;
            if(RankingPhraseList.containsDerankingPhrase(title)) {
                logger.debug("Found matching deranking phrase");
                importance = 2;
            }
        }

        return importance;
    }

    private static Map<Step, Boolean> createStepsMap() {
        Map<Step, Boolean> map = new HashMap<>();
        for(Step step : Step.values()) {
            map.put(step, false);
        }
        return map;
    }
}
