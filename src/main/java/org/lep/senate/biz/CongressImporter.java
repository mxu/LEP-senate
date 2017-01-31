package org.lep.senate.biz;

import com.beust.jcommander.JCommander;
import com.sun.tools.javac.util.Pair;
import org.lep.senate.dao.SenateDAO;
import org.lep.senate.loader.document.ActionsDocument;
import org.lep.senate.loader.document.AmendmentIndexDocument;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class CongressImporter {
    private static final Logger logger = LoggerFactory.getLogger(CongressImporter.class);

    private static final SenateDAO dao = new SenateDAO();

    private static final List<Pair<Integer, Integer>> BACKFILL_COUNTER = new ArrayList<>();
    private static final List<Pair<Integer, Integer>> LAW_COUNTER = new ArrayList<>();
    private static final List<Pair<Integer, Integer>> PASS_NOT_LAW_COUNTER = new ArrayList<>();
    private static final List<Pair<Integer, Integer>> AMENDED_COUNTER = new ArrayList<>();

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

        logCounter("Backfill PASS -> ABC", BACKFILL_COUNTER);
        logCounter("Matched LAW", LAW_COUNTER);
        logCounter("Matched PASS but not LAW", PASS_NOT_LAW_COUNTER);
        logCounter("Amended", AMENDED_COUNTER);
    }

    private static void logCounter(String name, List<Pair<Integer, Integer>> bills) {
        logger.info("{}: {} bills", name, bills.size());
        for(Pair<Integer, Integer> bill : bills) {
            logger.info("{} ({})", bill.snd, bill.fst);
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

        // NOTE(mike.xu): automatically set BILL step for now
        stepsMatched.put(Step.BILL, true);

        Set<Integer> amdtSponsorIds = new HashSet<>();
        for(String action : actions) {
            for(Step step : stepsMatched.keySet()) {
                if(!stepsMatched.get(step)) {
                    boolean matches = StepRegex.matchesStepRegex(step, action);
                    // NOTE(mike.xu): if bill achieves PASS, automatically set ABC as well
                    if(matches) {
                        stepsMatched.put(step, true);
                        if(step == Step.PASS && !stepsMatched.get(Step.ABC)) {
                            stepsMatched.put(Step.ABC, true);
                            logger.debug("backfill PASS -> ABC {} ({})", billNum, congressNum);
                            BACKFILL_COUNTER.add(new Pair<>(congressNum, billNum));
                        }
                    }
                }
            }

            boolean amended = StepRegex.matchesAmendedRegex(action);

            if(amended) {
                AMENDED_COUNTER.add(new Pair<>(congressNum, billNum));
                String amdtNum = AmendmentIndexDocument.getAmdtNum(action);
                Integer amdtSponsorId = dao.getAmdtSponsor(congressNum, amdtNum);
                amdtSponsorIds.add(amdtSponsorId);
            }
        }
        int amenders = amdtSponsorIds.size();

        if(stepsMatched.get(Step.LAW)) {
            logger.debug("match LAW {} ({})", billNum, congressNum);
            LAW_COUNTER.add(new Pair<>(congressNum, billNum));
        } else if(stepsMatched.get(Step.PASS)) {
            logger.debug("match PASS but not LAW {} ({})", billNum, congressNum);
            PASS_NOT_LAW_COUNTER.add(new Pair<>(congressNum, billNum));
        }

        if(!stepsMatched.get(Step.BILL)) {
            logger.warn("{} ({}) did not match BILL", billNum, congressNum);
        }

        dao.createBill(congressNum, billNum, title, importance, s.getId(), stepsMatched, amenders);
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
