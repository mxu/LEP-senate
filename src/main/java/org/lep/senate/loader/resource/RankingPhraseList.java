package org.lep.senate.loader.resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class RankingPhraseList {
    private static final Logger logger = LoggerFactory.getLogger(RankingPhraseList.class);

    private static final String RANKING_PHRASES_FILE = "ranking_phrases.csv";

    private static List<String> rankingPhrases = null;
    private static List<String> derankingPhrases = null;

    public static void main(String[] args) {
        try {
            List<String> rp = getRankingPhrases();
            logger.info("{} Ranking Phrases:", rp.size());
            rp.forEach(logger::info);

            List<String> drp = getDerankingPhrases();
            logger.info("{} Deranking Phrases:", drp.size());
            drp.forEach(logger::info);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static List<String> getRankingPhrases() throws FileNotFoundException {
        if(rankingPhrases == null) loadRankingPhrases();
        return rankingPhrases;
    }

    public static boolean containsRankingPhrase(String title) throws FileNotFoundException {
        for(String phrase : getRankingPhrases()) {
            if(title.contains(phrase)) {
                logger.debug("\"{}\" contains ranking phrase \"{}\"", title, phrase);
                return true;
            }
        }
        return false;
    }

    public static List<String> getDerankingPhrases() throws FileNotFoundException {
        if(derankingPhrases == null) loadRankingPhrases();
        return derankingPhrases;
    }

    public static boolean containsDerankingPhrase(String title) throws FileNotFoundException {
        for(String phrase : getDerankingPhrases()) {
            if(title.contains(phrase)) {
                logger.debug("\"{}\" contains deranking phrase \"{}\"", title, phrase);
                return true;
            }
        }
        return false;
    }
    private static void loadRankingPhrases() throws FileNotFoundException {
        rankingPhrases = new ArrayList<>();
        derankingPhrases = new ArrayList<>();

        List<String> lines = ResourceLoader.asList(RANKING_PHRASES_FILE);
        List<List<String>> phrases = lines.stream()
                .skip(1)
                .map(line -> Arrays.asList(line.split(",")))
                .collect(Collectors.toList());

        for(List<String> row : phrases) {
            String phrase = row.get(1).replace("\"", "");
            if(row.get(2).equals("0")) {
                rankingPhrases.add(phrase);
            } else {
                derankingPhrases.add(phrase);
            }
        }
    }
}
