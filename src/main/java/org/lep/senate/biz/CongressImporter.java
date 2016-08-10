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

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
                }
            }
        }
    }

    private static void processCongresses() {
        if (!canLoadStepRegex()) return;
        if (!canLoadRankingPhrases()) return;

        for(File congressFolder : DocumentLoader.getCongressFolders()) {
            try {
                Integer congressNum = Integer.parseInt(congressFolder.getName());
                processCongress(congressNum);
            } catch(NumberFormatException e) {
                logger.error("Invalid congress folder name \"{}\"", congressFolder.getName());
            }
        }
    }

    private static void processCongress(int congressNum) {
        logger.info("Processing congress {}...", congressNum);

        if (!canLoadStepRegex()) return;
        if (!canLoadRankingPhrases()) return;
        if (!canLoadImportantBills(congressNum)) return;
        Congress c = dao.getOrAddCongress(congressNum);

        int billsProcessed = 0;
        int billsFailed = 0;

        long startTime = System.nanoTime();
        for(File billFolder : DocumentLoader.getBillFolders(c.getId())) {
           try {
               Integer billNum = Integer.parseInt(billFolder.getName());
               try {
                   processBill(c.getId(), billNum);
                   billsProcessed++;
               } catch(Exception e) {
                   logger.error("Failed to process bill {} ({}): {}", billNum, congressNum, e.getMessage());
                   billsFailed++;
               }
           } catch(NumberFormatException e) {
               logger.error("Invalid bill folder name \"{}\"", billFolder.getName());
           }
        }
        long endTime = System.nanoTime();
        long totalTime = (endTime - startTime) / 1000000000;

        logger.info("Finished processing {}/{} bills in {}s", billsProcessed, billsFailed, totalTime);
    }

    private static void processBill(int congressNum, int billNum)
            throws FileNotFoundException, ParseFieldException, MissingSenatorException, MissingActionException {
        HeaderDocument headerDoc = new HeaderDocument(congressNum, billNum);
        String title = headerDoc.getTitle();

        String[] sponsor = headerDoc.getSponsor();
        Senator s = dao.getSenator(sponsor);
        if(s == null) {
            throw new MissingSenatorException(String.format("Unable to find senator (%s) %s %s",
                    sponsor[2], sponsor[0], sponsor[1]));
        }

        int importance = getImportance(congressNum, billNum, title);

        Map<Step, Boolean> stepsMatched = createStepsMap();
        ActionsDocument actionDoc = new ActionsDocument(congressNum, billNum);
        for(String action : actionDoc.getActions()) {
            for(Step step : stepsMatched.keySet()) {
                if(!stepsMatched.get(step)) {
                    stepsMatched.put(step, StepRegex.matchesStepRegex(step, action));
                }
            }
        }

        dao.createBill(congressNum, billNum, title, importance, s.getId(), stepsMatched);
    }

    private static int getImportance(int congressNum, int billNum, String title) throws FileNotFoundException {
        int importance = 2;

        List<Integer> importantBills = ImportantBillsList.getImportantBills(congressNum);
        if(importantBills.contains(billNum)) {
            importance = 3;
        } else if(RankingPhraseList.containsRankingPhrase(title)) {
            importance = 1;
            if(RankingPhraseList.containsDerankingPhrase(title)) {
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

    private static boolean canLoadRankingPhrases() {
        try {
            RankingPhraseList.getRankingPhrases();
            RankingPhraseList.getDerankingPhrases();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private static boolean canLoadImportantBills(int congressNum) {
        try {
            ImportantBillsList.getImportantBills(congressNum);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private static boolean canLoadStepRegex() {
        try {
            for(Step step : Step.values()) {
                StepRegex.getRegexList(step);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }
}
