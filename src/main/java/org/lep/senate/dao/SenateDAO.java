package org.lep.senate.dao;

import com.mysql.jdbc.jdbc2.optional.MysqlDataSource;
import org.lep.senate.model.Congress;
import org.lep.senate.model.Senator;
import org.lep.senate.model.Step;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SenateDAO {
    private static final Logger logger = LoggerFactory.getLogger(SenateDAO.class);

    private static final String DB_SERVER = "localhost";
    private static final int DB_PORT = 3306;
    private static final String DB_NAME = "lep-senate";
    private static final String DB_USER = "root";
    private static final String DB_PASS = "";

    private MysqlDataSource ds = null;

    public Connection getConnection() {
        Connection conn = null;
        try {
            if(ds == null) {
                ds = new MysqlDataSource();
                ds.setServerName(DB_SERVER);
                ds.setPort(DB_PORT);
                ds.setDatabaseName(DB_NAME);
                ds.setUser(DB_USER);
                ds.setPassword(DB_PASS);
            }
            conn = ds.getConnection();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return conn;
    }

    public void resetBills() {
        String deleteSql = "DELETE FROM bills;";
        String resetSql = "ALTER TABLE bills AUTO_INCREMENT = 1;";

        try(Connection conn = getConnection();
            Statement delete = conn.createStatement();
            Statement reset = conn.createStatement()) {
            delete.executeUpdate(deleteSql);
            reset.executeUpdate(resetSql);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public Congress getOrAddCongress(int congressNum) {
        Congress c = null;

        try(Connection conn = getConnection();
            PreparedStatement insert = createCongressInsert(conn, congressNum);
            PreparedStatement select = createCongressSelect(conn, congressNum)) {
            insert.execute();
            try(ResultSet rs = select.executeQuery()) {
                if(rs.next()) c = new Congress(rs.getInt(1));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return c;
    }

    private PreparedStatement createCongressInsert(Connection conn, int id) throws SQLException {
        String sql = "INSERT IGNORE INTO congresses (id) VALUE (?)";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setInt(1, id);
        return ps;
    }

    private PreparedStatement createCongressSelect(Connection conn, int id) throws SQLException {
        String sql = "SELECT * FROM congresses WHERE id=?";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setInt(1, id);
        return ps;
    }

    public Senator getSenator(String[] sponsorInfo) {
        Senator s = null;

        try (Connection conn = getConnection();
             PreparedStatement select = createSenatorSelect(conn, sponsorInfo)) {
            try (ResultSet rs = select.executeQuery()) {
                if (rs.next()) {
                    s = new Senator(
                            rs.getInt(1),
                            rs.getString(2),
                            rs.getString(3),
                            rs.getString(4),
                            rs.getString(5),
                            rs.getString(6));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return s;
    }

    private PreparedStatement createSenatorSelect(Connection conn, String[] sponsorInfo) throws SQLException {
        String sql = "SELECT * FROM senators WHERE first_name=? AND last_name=? AND state=?";
        PreparedStatement ps = conn.prepareStatement(sql);

        // NOTE(mike.xu): temporary hack for Blanche Lincoln/Lambert
        if(sponsorInfo[0].equals("Blanche") && sponsorInfo[1].equals("Lincoln")) {
            sponsorInfo[1] = "Lambert";
        }

        for(int i = 0; i < 3; i++) {
            ps.setString(i + 1, sponsorInfo[i]);
        }
        return ps;
    }

    private PreparedStatement createSenatorSelect(Connection conn, int senatorId) throws SQLException {
        String sql = "SELECT * FROM senators WHERE id=?";
        PreparedStatement ps = conn.prepareStatement(sql);

        ps.setInt(1, senatorId);
        return ps;
    }

    public Senator getSenator(int senatorId) {
        Senator s = null;

        try (Connection conn = getConnection();
             PreparedStatement select = createSenatorSelect(conn, senatorId)) {
            try (ResultSet rs = select.executeQuery()) {
                if (rs.next()) {
                    s = new Senator(
                            rs.getInt(1),
                            rs.getString(2),
                            rs.getString(3),
                            rs.getString(4),
                            rs.getString(5),
                            rs.getString(6));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return s;
    }

    public Integer createBill(int congressId, int billNum, String title, int importance, int sponsorId, Map<Step, Boolean> stepsMatched) {
        Integer billId = null;

        try(Connection conn = getConnection();
            PreparedStatement insert = createBillInsert(conn, congressId, billNum, title, importance, sponsorId, stepsMatched)) {
            insert.executeUpdate();
            try(ResultSet rs = insert.getGeneratedKeys()) {
               if(rs.next()) billId = rs.getInt(1);
            }
        } catch(SQLException e) {
            e.printStackTrace();
        }

        return billId;
    }

    private PreparedStatement createBillInsert(Connection conn, int congressId, int billNum, String title,
                                               int importance, int sponsorId, Map<Step, Boolean> stepsMatched) throws SQLException {
        String sql = "INSERT INTO bills (congress_id, num, sponsor_id, title, importance, AIC, ABC, BILL, PASS, LAW) VALUES (?,?,?,?,?,?,?,?,?,?)";
        PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
        ps.setInt(1, congressId);
        ps.setInt(2, billNum);
        ps.setInt(3, sponsorId);
        ps.setString(4, title);
        ps.setInt(5, importance);
        ps.setInt(6, stepsMatched.get(Step.AIC) ? 1 : 0);
        ps.setInt(7, stepsMatched.get(Step.ABC) ? 1 : 0);
        ps.setInt(8, stepsMatched.get(Step.BILL) ? 1 : 0);
        ps.setInt(9, stepsMatched.get(Step.PASS) ? 1 : 0);
        ps.setInt(10, stepsMatched.get(Step.LAW) ? 1 : 0);
        return ps;
    }

    public List<Integer> getCongresses() {
        String selectSql = "SELECT id FROM congresses ORDER BY id";
        List<Integer> congressIds = new ArrayList<>();

        try(Connection conn = getConnection();
            Statement select = conn.createStatement()) {
            try (ResultSet rs = select.executeQuery(selectSql)) {
                while(rs.next()) {
                    congressIds.add(rs.getInt(1));
                }
            }
        } catch(SQLException e) {
            e.printStackTrace();
        }

        return congressIds;
    }

    private PreparedStatement createSenatorsSelect(Connection conn, int congressId) throws SQLException {
        String sql = "SELECT senator_id FROM congresses_senators WHERE congress_id=?";
        PreparedStatement ps = conn.prepareStatement(sql);

        ps.setInt(1, congressId);
        return ps;
    }

    public List<Integer> getSenatorIds(int congressId) {
        List<Integer> senatorIds = new ArrayList<>();

        try(Connection conn = getConnection();
            PreparedStatement select = createSenatorsSelect(conn, congressId)) {
            try(ResultSet rs = select.executeQuery()) {
                while(rs.next()) {
                    senatorIds.add(rs.getInt(1));
                }
            }
        } catch(SQLException e) {
            e.printStackTrace();
        }

        return senatorIds;
    }

    private PreparedStatement getBillSelect(Connection conn, int congressId, int senatorId, int importance, Step step) throws SQLException {
        String sql = "SELECT count(*) FROM bills WHERE congress_id=? AND sponsor_id=? AND importance=? AND " + step.name() + "=1";
        PreparedStatement ps = conn.prepareStatement(sql);

        ps.setInt(1, congressId);
        ps.setInt(2, senatorId);
        ps.setInt(3, importance);
        return ps;
    }

    public int getBillCount(int congressId, int senatorId, int importance, Step step) {
        int count = 0;

        try(Connection conn = getConnection();
            PreparedStatement select = getBillSelect(conn, congressId, senatorId, importance, step)) {
            try(ResultSet rs = select.executeQuery()) {
                if(rs.next()) {
                    count = rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return count;
    }

    private PreparedStatement getSenatorSelect(Connection conn, int senatorId) throws SQLException {
        String sql = "SELECT first_name, last_name FROM senators WHERE id=?";
        PreparedStatement ps = conn.prepareStatement(sql);

        ps.setInt(1, senatorId);
        return ps;
    }

    public String getSenatorName(int senatorId) {
        StringBuilder sb = new StringBuilder();

        try(Connection conn = getConnection();
            PreparedStatement select = getSenatorSelect(conn, senatorId)) {
            try(ResultSet rs = select.executeQuery()) {
                if(rs.next()) {
                    sb.append(rs.getString(1));
                    sb.append(" ");
                    sb.append(rs.getString(2));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return sb.toString();
    }
}
