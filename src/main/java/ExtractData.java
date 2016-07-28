import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
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

    private static final Pattern TITLE_PATTERN = Pattern.compile("S.\\d+ - (.*) \\d+th Congress \\(\\d+-\\d+\\)");
    private static final Pattern SPONSOR_PATTERN = Pattern.compile("Sen. (\\w+), (\\w+) \\[(\\w)-(\\w\\w)]");

    private static final SenateDAO dao = new SenateDAO();

    private static List<List<String>> rankingPhrases = null;
    private static Map<Integer, List<Integer>> importantBills = new HashMap<>();

    public static void main(String[] args) {
//        processCongresses();
        Congress c = dao.getOrAddCongress(113);
        processBill(c, 100);
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

        String congressPath = DATA_ROOT + "/" + c.getNum();
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
        // ranking phrases
        // important lists

        // language regex

        Document actions = getBillTab(c.getNum(), billNum, BillTab.ACTIONS);
        Document header = getBillTab(c.getNum(), billNum, BillTab.HEADER);

        String title = getTitle(header);

        int importance = 2;
        if(getImportantBills(c.getNum()).contains(billNum)) {
            importance = 3;
        } else if(containsRankingPhrase(title)) {
            importance = 1;
        }

        String[] sponsorInfo = getSponsor(header);
        if (sponsorInfo != null) {
            Arrays.stream(sponsorInfo).forEach(System.out::println);
        }
        System.out.println(title);
        System.out.println(importance);
        // Senator s = dao.getSenator(sponsorName);
    }

    private static String[] getSponsor(Document header) {
        String text = header.select("tr:nth-child(1) a").text();
        Matcher matcher = SPONSOR_PATTERN.matcher(text);
        if(matcher.find()) {
            String first = matcher.group(2);
            String last = matcher.group(1);
            String party = matcher.group(3);
            String state = matcher.group(4);
            return new String[]{first, last, party, state};
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
        List<List<String>> phrases = getRankingPhrases();
        for(List<String> phrase : phrases) {
            if(title.contains(phrase.get(1))) return true;
        }

        return false;
    }

    private static List<List<String>> getRankingPhrases() {
        if(rankingPhrases == null) {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            InputStream is = classLoader.getResourceAsStream(RANKING_PHRASES_FILE);
            BufferedReader br = new BufferedReader(new InputStreamReader(is));

            rankingPhrases = br.lines()
                    .skip(1)
                    .map(line -> Arrays.asList(line.split(",")))
                    .filter(line -> line.get(2).equals("0"))
                    .collect(Collectors.toList());
        }

        return rankingPhrases;
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
