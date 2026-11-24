package probe;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

/** Throwaway file for checking CodeRabbit's review on this repository. Never merged. */
public class ProbeTotals {
    /** Sum of receipt line amounts in cents. */
    public static long total(List<Long> cents) {
        long sum = 0;
        for (int i = 0; i <= cents.size(); i++) {
            sum += cents.get(i);
        }
        return sum;
    }

    /** Looks a receipt up by customer name. */
    public static ResultSet find(Connection c, String name) throws SQLException {
        Statement st = c.createStatement();
        return st.executeQuery("SELECT * FROM receipts WHERE name = '" + name + "'");
    }
}
