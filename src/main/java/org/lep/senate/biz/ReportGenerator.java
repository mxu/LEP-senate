package org.lep.senate.biz;

import com.beust.jcommander.JCommander;
import org.lep.senate.dao.SenateDAO;
import org.lep.senate.model.ReportRow;
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
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
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
        generateTotalReport(congressId);
        if(congressId > 96) {
            generateUnamendedReport(congressId);
            generateAmendedReport(congressId);
            generateAltAmendedReport(congressId);
        }
    }

    private static void buildReport(Map<Integer, ReportRow> report, ResultSet rs) throws SQLException {
        while(rs.next()) {
            int senatorId = rs.getInt(1);
            ReportRow row = report.get(senatorId);
            if (row == null) {
                row = new ReportRow();
                report.put(senatorId, row);
            }

            int importance = rs.getInt(2);
            if (rs.getInt(3) == 1) row.incrementScore(importance, Step.BILL);
            if (rs.getInt(4) == 1) row.incrementScore(importance, Step.AIC);
            if (rs.getInt(5) == 1) row.incrementScore(importance, Step.ABC);
            if (rs.getInt(6) == 1) row.incrementScore(importance, Step.PASS);
            if (rs.getInt(7) == 1) row.incrementScore(importance, Step.LAW);
        }
    }

    private static PreparedStatement createTotalReportSelect(Connection conn, int congressId) throws SQLException {
        String sql = "SELECT sponsor_id, importance, BILL, AIC, ABC, PASS, LAW FROM bills WHERE congress_id=?";
        //                   1           2           3     4    5    6     7
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setInt(1, congressId);
        return ps;
    }

    private static void generateTotalReport(int congressId) {
        Map<Integer, ReportRow> report = new HashMap<>();

        try(Connection conn = dao.getConnection();
            PreparedStatement select = createTotalReportSelect(conn, congressId)) {
            try(ResultSet rs = select.executeQuery()) {
                buildReport(report, rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        writeReport(report, "Report_" + congressId + "_total.tsv");
    }

    private static PreparedStatement createUnamendedReportSelect(Connection conn, int congressId) throws SQLException {
        String sql = "SELECT sponsor_id, importance, BILL, AIC, ABC, PASS, LAW FROM bills WHERE congress_id=? AND amenders=0";
        //                   1           2           3     4    5    6     7
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setInt(1, congressId);
        return ps;
    }

    private static void generateUnamendedReport(int congressId) {
        Map<Integer, ReportRow> report = new HashMap<>();

        try(Connection conn = dao.getConnection();
            PreparedStatement select = createUnamendedReportSelect(conn, congressId)) {
            try(ResultSet rs = select.executeQuery()) {
                buildReport(report, rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        writeReport(report, "Report_" + congressId + "_unamended.tsv");
    }

    private static PreparedStatement createAmendedReportSelect(Connection conn, int congressId) throws SQLException {
        String sql = "SELECT sponsor_id, importance, BILL, AIC, ABC, PASS, LAW, num, amenders FROM bills WHERE congress_id=? AND amenders>0";
        //                   1           2           3     4    5    6     7    8    9
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setInt(1, congressId);
        return ps;
    }

    private static PreparedStatement createAmendersSelect(Connection conn, int congressId, int billNum) throws SQLException {
        String sql = "SELECT DISTINCT(sponsor_id) FROM amendment_sponsors WHERE congress_id=? AND bill_num=? AND successful=1";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setInt(1, congressId);
        ps.setInt(2, billNum);
        return ps;
    }

    private static void generateAmendedReport(int congressId) {
        Map<Integer, ReportRow> report = new HashMap<>();

        try(Connection conn = dao.getConnection();
            PreparedStatement select = createAmendedReportSelect(conn, congressId)) {
            try(ResultSet rs = select.executeQuery()) {
                while(rs.next()) {
                    int sponsorId = rs.getInt(1);
                    ReportRow row = report.get(sponsorId);
                    if(row == null) {
                        row = new ReportRow();
                        report.put(sponsorId, row);
                    }

                    int importance = rs.getInt(2);
                    int billNum = rs.getInt(8);
                    int amenders = rs.getInt(9);
                    double amenderScore = 0.5 * 1/amenders;

                    List<ReportRow> amenderRows = new ArrayList<>();
                    try(PreparedStatement selectAmenders = createAmendersSelect(conn, congressId, billNum)) {
                        try(ResultSet amendersRs = selectAmenders.executeQuery()) {
                            while(amendersRs.next()) {
                                int amenderId = amendersRs.getInt(1);

                                ReportRow amenderRow = report.get(amenderId);
                                if(amenderRow == null) {
                                    amenderRow = new ReportRow();
                                    report.put(amenderId, amenderRow);
                                }
                                amenderRows.add(amenderRow);
                            }
                        }
                    }

                    if(rs.getInt(3) == 1) {
                        row.incrementScore(importance, Step.BILL, 0.5);
                        for(ReportRow amenderRow : amenderRows) {
                            amenderRow.incrementScore(importance, Step.BILL, amenderScore);
                        }
                    }
                    if(rs.getInt(4) == 1) {
                        row.incrementScore(importance, Step.AIC, 0.5);
                        for(ReportRow amenderRow : amenderRows) {
                            amenderRow.incrementScore(importance, Step.AIC, amenderScore);
                        }
                    }
                    if(rs.getInt(5) == 1) {
                        row.incrementScore(importance, Step.ABC, 0.5);
                        for(ReportRow amenderRow : amenderRows) {
                            amenderRow.incrementScore(importance, Step.ABC, amenderScore);
                        }
                    }
                    if(rs.getInt(6) == 1) {
                        row.incrementScore(importance, Step.PASS, 0.5);
                        for(ReportRow amenderRow : amenderRows) {
                            amenderRow.incrementScore(importance, Step.PASS, amenderScore);
                        }
                    }
                    if(rs.getInt(7) == 1) {
                        row.incrementScore(importance, Step.LAW, 0.5);
                        for(ReportRow amenderRow : amenderRows) {
                            amenderRow.incrementScore(importance, Step.LAW, amenderScore);
                        }
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        writeReport(report, "Report_" + congressId + "_amended.tsv");
    }

    private static PreparedStatement createAltAmendersSelect(Connection conn, int congressId, int billNum) throws SQLException {
        String sql = "SELECT sponsor_id FROM amendment_sponsors WHERE congress_id=? AND bill_num=? AND successful=1";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setInt(1, congressId);
        ps.setInt(2, billNum);
        return ps;
    }

    private static void generateAltAmendedReport(int congressId) {
        Map<Integer, ReportRow> report = new HashMap<>();

        try(Connection conn = dao.getConnection();
            PreparedStatement select = createAmendedReportSelect(conn, congressId)) {
            try(ResultSet rs = select.executeQuery()) {
                while(rs.next()) {
                    int sponsorId = rs.getInt(1);
                    ReportRow row = report.get(sponsorId);
                    if(row == null) {
                        row = new ReportRow();
                        report.put(sponsorId, row);
                    }

                    int importance = rs.getInt(2);
                    int billNum = rs.getInt(8);
                    Map<Integer, Double> amenderScores = new HashMap<>();
                    int successfulAmendments = 0;

                    try(PreparedStatement selectAmenders = createAltAmendersSelect(conn, congressId, billNum)) {
                        try(ResultSet amendersRs = selectAmenders.executeQuery()) {
                            while(amendersRs.next()) {
                                successfulAmendments++;
                                int amenderId = amendersRs.getInt(1);

                                ReportRow amenderRow = report.get(amenderId);
                                if(amenderRow == null) {
                                    amenderRow = new ReportRow();
                                    report.put(amenderId, amenderRow);
                                }

                                if(amenderScores.containsKey(amenderId)) {
                                    amenderScores.put(amenderId, amenderScores.get(amenderId) + 1);
                                } else {
                                    amenderScores.put(amenderId, 1.0);
                                }
                            }

                            for(Entry<Integer, Double> amenderScore : amenderScores.entrySet()) {
                                amenderScores.put(amenderScore.getKey(), 0.5 * amenderScore.getValue() / successfulAmendments);
                            }
                        }
                    }

                    if(rs.getInt(3) == 1) {
                        row.incrementScore(importance, Step.BILL, 0.5);
                        for(Integer amenderId : amenderScores.keySet()) {
                            ReportRow amenderRow = report.get(amenderId);
                            Double amenderScore = amenderScores.get(amenderId);
                            amenderRow.incrementScore(importance, Step.BILL, amenderScore);
                        }
                    }

                    if(rs.getInt(4) == 1) {
                        row.incrementScore(importance, Step.AIC, 0.5);
                        for(Integer amenderId : amenderScores.keySet()) {
                            ReportRow amenderRow = report.get(amenderId);
                            Double amenderScore = amenderScores.get(amenderId);
                            amenderRow.incrementScore(importance, Step.AIC, amenderScore);
                        }
                    }
                    if(rs.getInt(5) == 1) {
                        row.incrementScore(importance, Step.ABC, 0.5);
                        for(Integer amenderId : amenderScores.keySet()) {
                            ReportRow amenderRow = report.get(amenderId);
                            Double amenderScore = amenderScores.get(amenderId);
                            amenderRow.incrementScore(importance, Step.ABC, amenderScore);
                        }
                    }
                    if(rs.getInt(6) == 1) {
                        row.incrementScore(importance, Step.PASS, 0.5);
                        for(Integer amenderId : amenderScores.keySet()) {
                            ReportRow amenderRow = report.get(amenderId);
                            Double amenderScore = amenderScores.get(amenderId);
                            amenderRow.incrementScore(importance, Step.PASS, amenderScore);
                        }
                    }
                    if(rs.getInt(7) == 1) {
                        row.incrementScore(importance, Step.LAW, 0.5);
                        for(Integer amenderId : amenderScores.keySet()) {
                            ReportRow amenderRow = report.get(amenderId);
                            Double amenderScore = amenderScores.get(amenderId);
                            amenderRow.incrementScore(importance, Step.LAW, amenderScore);
                        }
                    }
                }
            }
        } catch(SQLException e) {
            e.printStackTrace();
        }

        writeReport(report, "Report_" + congressId + "_amended_alt.tsv");
    }

    private static void writeReport(Map<Integer, ReportRow> report, String fileName) {
        List<String> lines = new ArrayList<>();

        StringBuilder headerSb = new StringBuilder();
        headerSb.append("LAST\tFIRST\tSTATE");
        for(int importance = 1; importance < 4; importance++) {
            for(Step step : Step.values()) {
                headerSb.append("\t");
                headerSb.append(step.name());
                headerSb.append(importance);
            }
        }
        lines.add(headerSb.toString());

        for(Entry<Integer, ReportRow> entry : report.entrySet()) {
            Integer senatorId = entry.getKey();
            Senator s = dao.getSenator(senatorId);
            StringBuilder sb = new StringBuilder();
            sb.append(s.getLastName());
            sb.append("\t");
            sb.append(s.getFirstName());
            sb.append("\t");
            sb.append(s.getState());

            ReportRow row = entry.getValue();
            for(int importance = 1; importance < 4; importance++) {
                for(Step step : Step.values()) {
                    sb.append("\t");
                    sb.append(row.getScore(importance, step));
                }
            }

            lines.add(sb.toString());
        }

        Path file = Paths.get("reports/" + fileName);
        try {
            Files.write(file, lines, Charset.forName("UTF-8"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        logger.info("Wrote {}", file.toAbsolutePath().toString());
    }
}

