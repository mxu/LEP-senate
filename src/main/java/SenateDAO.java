
import com.mysql.jdbc.jdbc2.optional.MysqlDataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class SenateDAO {
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

    public Congress getOrAddCongress(int congressNum) {
        Congress c = null;

        try(Connection conn = getConnection();
            PreparedStatement insert = createCongressInsert(conn, congressNum);
            PreparedStatement select = createCongressSelect(conn, congressNum)) {
            insert.execute();
            try(ResultSet rs = select.executeQuery()) {
                if(rs.next()) c = new Congress(rs.getInt(1), rs.getInt(2));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return c;
    }

    private PreparedStatement createCongressInsert(Connection conn, int num) throws SQLException {
        String sql = "INSERT IGNORE INTO congresses (num) VALUE (?)";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setInt(1, num);
        return ps;
    }

    private PreparedStatement createCongressSelect(Connection conn, int num) throws SQLException {
        String sql = "SELECT * FROM congresses WHERE num=?";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setInt(1, num);
        return ps;
    }

    /*
    public Senator getSenator(Congress c, String name) {
        Senator s = null;

        try(Connection conn = getConnection();
            PreparedStatement select = createSenatorSelect(conn, c, name)) {
            try(ResultSet rs = select.executeQuery()) {
                if(rs.next()) s = new Senator();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return s;
    }

    private PreparedStatement createSenatorSelect(Connection conn, Congress c, String name) throws SQLException {
        String sql = "SELECT * FROM senators WHERE first_name=? AND last_name=? AND state=?";
        PreparedStatement ps = conn.prepareStatement(sql);
        // TODO(mike.xu): parse name into first, last, state
        String first = null;
        String last = null;
        String state = null;
        ps.setString(1, first);
        ps.setString(2, last);
        ps.setString(3, state);

        return ps;
    }
    */

    public List<Integer> listCongresses() {
        List<Integer> congresses = new ArrayList<>();

        Connection conn = null;
        Statement st = null;
        ResultSet rs = null;
        try {
            conn = getConnection();
            st = conn.createStatement();
            rs = st.executeQuery("SELECT num FROM congresses");
            while(rs.next()) {
                congresses.add(rs.getInt(1));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            if(conn != null) { try { conn.close(); } catch (SQLException e) {e.printStackTrace(); } }
            if(st != null) { try { st.close(); } catch (SQLException e) { e.printStackTrace(); } }
            if(rs != null) { try { rs.close(); } catch (SQLException e) { e.printStackTrace(); } }
        }

        return congresses;
    }
}
