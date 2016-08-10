package org.lep.senate.loader.resource;

import com.beust.jcommander.JCommander;
import org.lep.settings.CongressSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ImportantBillsList {
    private static final Logger logger = LoggerFactory.getLogger(ImportantBillsList.class);

    private static final String IMPORTANT_LIST_FORMAT = "%d_senate_important.txt";

    private static Map<Integer, List<Integer>> importantBills = new HashMap<>();

    public static void main(String[] args) {
        CongressSettings settings = new CongressSettings();
        new JCommander(settings, args);

        Integer congress = settings.getCongress();
        if(congress == null) {
            logger.error("Please specify a congress with -c");
            return;
        }

        try {
            List<Integer> importantBills = getImportantBills(congress);
            logger.info("{} important bills for congress {}:", importantBills.size(), congress);
            importantBills.forEach(number -> logger.info(number.toString()));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static List<Integer> getImportantBills(int congressNum) throws FileNotFoundException {
        if(!importantBills.containsKey(congressNum)) {
            List<String> lines = ResourceLoader.asList(String.format(IMPORTANT_LIST_FORMAT, congressNum));

            importantBills.put(congressNum,
                    lines.stream()
                            .map(Integer::parseInt)
                            .collect(Collectors.toList()));
        }

        return importantBills.get(congressNum);
    }
}
