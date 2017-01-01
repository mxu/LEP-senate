package org.lep.senate.biz;

import com.beust.jcommander.JCommander;
import org.lep.senate.dao.SenateDAO;
import org.lep.senate.model.Senator;
import org.lep.senate.model.Step;
import org.lep.settings.ReportSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class ReportGenerator {
    private static final Logger logger = LoggerFactory.getLogger(ReportGenerator.class);

    private static final SenateDAO dao = new SenateDAO();

    public static void main(String[] args) {
        ReportSettings settings = new ReportSettings();
        new JCommander(settings, args);

        Integer congressNum = settings.getCongress();

        if(congressNum == null) {
            generateReports();
        } else {
            try {
                generateReport(congressNum);
            } catch (Exception e) {
                logger.error("Failed to generate report for congress {}: {}", congressNum, e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private static void generateReports() {
        for(Integer congressNum : dao.getCongresses()) {
            try {
                generateReport(congressNum);
            } catch (Exception e) {
                logger.error("Failed to generate report for congress {}: {}", congressNum, e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private static void generateReport(int congressId) {
        Map<Senator, List<Integer>> report = new HashMap<>();

        for(Integer senatorId : dao.getSenatorIds(congressId)) {
            List<Integer> values = new ArrayList<>();

            for(int importance = 1 ; importance < 4 ; importance++) {
                for(Step step : Step.values()) {
                    values.add(dao.getBillCount(congressId, senatorId, importance, step));
                }
            }

            report.put(dao.getSenator(senatorId), values);
        }

        List<String> lines = new ArrayList<>();

        StringBuilder headerSb = new StringBuilder();
        headerSb.append("LAST\tFIRST\tSTATE");
        for(int importance = 1 ; importance < 4 ; importance++) {
            for(Step step : Step.values()) {
                headerSb.append("\t");
                headerSb.append(step.name());
                headerSb.append(importance);
            }
        }
        lines.add(headerSb.toString());

        for(Entry<Senator, List<Integer>> entry : report.entrySet()) {
            Senator s = entry.getKey();
            StringBuilder sb = new StringBuilder();
            sb.append(s.getLastName());
            sb.append("\t");
            sb.append(s.getFirstName());
            sb.append("\t");
            sb.append(s.getState());
            for(Integer i : entry.getValue()) {
                sb.append("\t");
                sb.append(i);
            }
            lines.add(sb.toString());
        }

        Path file = Paths.get("Report_" + congressId + ".tsv");
        try {
            Files.write(file, lines, Charset.forName("UTF-8"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        logger.info("Wrote {}", file.toAbsolutePath().toString());
    }
}
