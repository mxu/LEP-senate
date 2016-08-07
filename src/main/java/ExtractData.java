import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ExtractData {
    private static final String DATA_ROOT = "/Users/mike.xu/code/LEP-senate/data";
    private static final String CONGRESS_DIR_FORMAT = DATA_ROOT + "/%d";
    private static final String BILL_DIR_FORMAT = CONGRESS_DIR_FORMAT + "/%d";
    private static final String BILL_TAB_FORMAT = BILL_DIR_FORMAT + "/%s.html";

    private static final String IMPORTANT_LIST_FORMAT = "%d_senate_important.txt";
    private static final String RANKING_PHRASES_FILE = "ranking_phrases.csv";

    private static final Pattern TITLE_PATTERN = Pattern.compile("S.\\d+ - (.*) \\d+\\w+ Congress \\(\\d+-\\d+\\)");
    private static final Pattern SPONSOR_PATTERN = Pattern.compile("Sen. ([\\w\\s.]+), ([\\w.]+)( [\\w.]+)?(,? \\w+.)?( \\(\\w+\\))? \\[(\\w)-(\\w\\w)");

    private static final SenateDAO dao = new SenateDAO();

    private static List<String> rankingPhrases = null; // e=0: 2 -> 1
    private static List<String> derankingPhrases = null; // e=1: 1 -> 2
    private static Map<Integer, List<Integer>> importantBills = new HashMap<>();
    private static Map<Step, List<Pattern>> binPatterns = new HashMap<>();

    public static void main(String[] args) {
        dao.resetBills();

        // NOTE(mike.xu): look through data folders to populate congresses table
//        processCongresses();

        // NOTE(mike.xu): load ranking phrases from csv
//        getRankingPhrases();
//        System.out.println("rankingPhrases:");
//        for(String phrase : rankingPhrases) {
//            System.out.println(phrase);
//        }
//        System.out.println("derankingPhrases:");
//        for(String phrase : derankingPhrases) {
//            System.out.println(phrase);
//        }

        // NOTE(mike.xu): lazy load important lists
//        List<Integer> importantBills93 = getImportantBills(93);
//        for(Integer billId : importantBills93) {
//            System.out.println(billId);
//        }

        // NOTE(mike.xu): load bin regexes
//        List<Pattern> ABCPatterns = getBinPatterns(Step.ABC);
//        System.out.printf("ABC has " + ABCPatterns.size() + " patterns");
//        String test = "By Senator Leahy from Committee on the Judiciary filed written report under authority of the order of the Senate of 03/23/2013. Report No. 113-9. Additional and Minority views filed.";
//        System.out.println(test);

        // NOTE(mike.xu): populate bill table for a single bill
//        Congress c = dao.getOrAddCongress(93);
//        processBill(c, 1112);

        // NOTE(mike.xu): populate all bills for a single congress
        processCongress(94);
    }

    private static void processCongresses() {
        File[] congressFolders = new File(DATA_ROOT).listFiles(File::isDirectory);
        if(congressFolders == null) {
            System.out.println("ERROR: data directory contains no congress folders");
            return;
        }

        for(File congressFolder : congressFolders) {
            processCongress(Integer.parseInt(congressFolder.getName()));
        }
    }

    private static void processCongress(int congressNum) {
        Congress c = dao.getOrAddCongress(congressNum);

        String congressPath = DATA_ROOT + "/" + c.getId();
        File[] billFolders = new File(congressPath).listFiles(File::isDirectory);
        if(billFolders == null) {
            System.out.println("ERROR: congress directory " + congressNum + " contains no bill folders");
            return;
        }

        int numBills = billFolders.length;
        System.out.println(c + " has " + numBills + " bills");
        for(int billNum = 1; billNum <= numBills; billNum++) {
           processBill(c, billNum);
        }
    }

    private static void processBill(Congress c, int billNum) {
//        System.out.printf("Starting to parse bill %d\n", billNum);

        // load header html
        Document header = getBillTab(c.getId(), billNum, BillTab.HEADER);

        String title = getTitle(header);
//        System.out.printf("\tTitle: %s\n", title);

        int importance = getImportance(c.getId(), billNum, title);
//        System.out.printf("\tImportance: %d\n", importance);

        String[] sponsorInfo = getSponsor(header);
        if(sponsorInfo == null) {
            System.out.println("ERROR: missing sponsor for " + c.getId() + "," + billNum);
            return;
        }
        Senator s = dao.getSenator(sponsorInfo);
//        System.out.printf("\tSenator: %s\n", s);

        // load actions html
        Document allActions = getBillTab(c.getId(), billNum, BillTab.ACTIONS);
        List<String> actions = getActions(allActions);
//        System.out.printf("\tActions: %d\n", actions.size());

        Map<Step, Boolean> stepsMatched = new HashMap<>();
        for(String action : actions) {
            for(Step step : Step.values()) {
               if(!stepsMatched.containsKey(step) || !stepsMatched.get(step)) {
                   stepsMatched.put(step, matchStepRegexes(step, action));
               }
            }
        }

        Integer billId = dao.createBill(c.getId(), billNum, title, importance, s.getId(), stepsMatched);
        System.out.printf("Done parsing bill %d (id=%d)\n", billNum, billId);
    }

    private static boolean matchStepRegexes(Step step, String action) {
        for(Pattern p : getBinPatterns(step)) {
            if(p.matcher(action).find()) {
//                System.out.printf("\t\tMATCH (%s):\n\t\t%s\n\t\t%s\n", step.name(), action, p.pattern());
                return true;
            }
        }
        return false;
    }

    private static List<String> getActions(Document allActions) {
        List<String> actions = new ArrayList<>();
        for(Element e : allActions.select("td.actions")) {
            String action = e.text();
            actions.add(action);
        }
        return actions;
    }

    private static int getImportance(int congressNum, int billNum, String title) {
        int importance = 2;
        List<Integer> importantList = getImportantBills(congressNum);
        if(importantList.contains(billNum)) {
//            System.out.printf("\t\tMatches important list (%d)\n", billNum);
            importance = 3;
        } else if(containsRankingPhrase(title)) {
            importance = 1;
            if(containsDerankingPhrase(title)) {
                importance = 2;
            }
        }
        return importance;
    }

    private static String[] getSponsor(Document header) {
        String text = header.select("tr:nth-child(1) a").text();
        Matcher matcher = SPONSOR_PATTERN.matcher(text);
        if(matcher.find()) {
            String first = matcher.group(2);
            String middle = matcher.group(3);
//            middle = middle == null ? "" : middle.trim();
            String last = matcher.group(1);
            String suffix = matcher.group(4);
//            suffix = suffix == null ? "" : suffix.substring(2);
//            String nickname = matcher.group(5);
//            String party = matcher.group(6);
            String state = matcher.group(7);
            return new String[]{first, last, state};
        }
        System.out.println("ERROR: unable to parse sponsor from \"" + text + "\"");
        return null;
    }

    private static String getTitle(Document header) {
        String text = header.select(".legDetail").text();
        Matcher matcher = TITLE_PATTERN.matcher(text);
        if(matcher.find()) {
            return matcher.group(1);
        }

        System.out.println("ERROR: unable to parse title from \"" + text + "\"");
        return null;
    }

    private static List<Integer> getImportantBills(int congressNum) {
        if(!importantBills.containsKey(congressNum)) {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            InputStream is = classLoader.getResourceAsStream(String.format(IMPORTANT_LIST_FORMAT, congressNum));
            BufferedReader br = new BufferedReader(new InputStreamReader(is));

            importantBills.put(congressNum, br.lines()
                    .map(Integer::parseInt)
                    .collect(Collectors.toList()));
        }

        return importantBills.get(congressNum);
    }

    private static boolean containsRankingPhrase(String title) {
        for(String phrase : getRankingPhrases()) {
            if(title.contains(phrase)) {
//                System.out.printf("\t\tContains ranking phrase \"%s\":\n\t\t%s\n", phrase, title);
                return true;
            }
        }
        return false;
    }

    private static boolean containsDerankingPhrase(String title) {
        for(String phrase : getDerankingPhrases()) {
            if(title.contains(phrase)) {
//                System.out.printf("\t\tContains deranking phrase \"%s\":\n\t\t%s\n", phrase, title);
                return true;
            }
        }
        return false;
    }

    private static List<String> getRankingPhrases() {
        if(rankingPhrases == null) {
            loadRankingPhrases();

        }

        return rankingPhrases;
    }

    private static List<String> getDerankingPhrases() {
        if(derankingPhrases == null) {
           loadRankingPhrases();
        }

        return derankingPhrases;
    }

    private static void loadRankingPhrases() {
        rankingPhrases = new ArrayList<>();
        derankingPhrases = new ArrayList<>();

        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        InputStream is = classLoader.getResourceAsStream(RANKING_PHRASES_FILE);
        BufferedReader br = new BufferedReader(new InputStreamReader(is));

        List<List<String>> phraseData = br.lines()
                .skip(1)
                .map(line -> Arrays.asList(line.split(",")))
                .collect(Collectors.toList());

        for(List<String> row : phraseData) {
            if(row.get(2).equals("0")) {
                rankingPhrases.add(row.get(1));
            } else {
                derankingPhrases.add(row.get(1));
            }
        }
    }

    private static List<Pattern> getBinPatterns(Step step) {
        if(!binPatterns.containsKey(step)) {
            loadBinPatterns();
        }

        return binPatterns.get(step);
    }

    private static void loadBinPatterns() {
        for(Step step : Step.values()) {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            InputStream is = classLoader.getResourceAsStream(step.name() + ".txt");
            BufferedReader br = new BufferedReader(new InputStreamReader(is));

            List<Pattern> patterns = br.lines()
                    .map(line -> Pattern.compile(line.length() > 6 ? line.substring(1, line.length() - 3) : ""))
                    .collect(Collectors.toList());

            binPatterns.put(step, patterns);
        }
    }

    private static Document getBillTab(int congress, int bill, BillTab tab) {
        Document doc = null;

        String path = String.format(BILL_TAB_FORMAT, congress, bill, tab.getFileName());
        File input = new File(path);

        try {
            doc = Jsoup.parse(input, "UTF-8", "https://www.congress.gov");
        } catch(IOException e) {
            System.out.printf("Error parsing %s\n", path);
        }

        return doc;
    }
}
