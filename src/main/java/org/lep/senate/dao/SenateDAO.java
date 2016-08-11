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
}
