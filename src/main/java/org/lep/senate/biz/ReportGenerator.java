package org.lep.senate.biz;

import com.beust.jcommander.JCommander;
import org.lep.senate.dao.SenateDAO;
import org.lep.senate.model.Step;
import org.lep.settings.ReportSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
        Map<String, List<Integer>> report = new HashMap<>();

        for(Integer senatorId : dao.getSenatorIds(congressId)) {
            List<Integer> values = new ArrayList<>();

            for(int importance = 1 ; importance < 4 ; importance++) {
                for(Step step : Step.values()) {
                    values.add(dao.getBillCount(congressId, senatorId, importance, step));
                }
            }

            report.put(dao.getSenatorName(senatorId), values);
        }

        System.out.println("Congress " + congressId + ":");
        StringBuilder headerSb = new StringBuilder();
        headerSb.append("SENATOR");
        for(int importance = 1 ; importance < 4 ; importance++) {
            for(Step step : Step.values()) {
                headerSb.append("\t");
                headerSb.append(step.name());
                headerSb.append(importance);
            }
        }
        System.out.println(headerSb.toString());

        for(Entry<String, List<Integer>> entry : report.entrySet()) {
            StringBuilder sb = new StringBuilder();
            sb.append(entry.getKey());
            for(Integer i : entry.getValue()) {
                sb.append("\t");
                sb.append(i);
            }
            System.out.println(sb.toString());
        }
    }
}
