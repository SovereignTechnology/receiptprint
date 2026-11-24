package probe;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/** Throwaway file for checking CodeRabbit's review on this repository. Never merged. */
public class ProbeTotals {
    /** Sum of receipt line amounts in cents. */
    public static long total(List<Long> cents) {
        long sum = 0;
        for (int i = 0; i < cents.size(); i++) {
            sum += cents.get(i);
        }
        return sum;
    }

    /** Looks a receipt up by customer name. The caller owns the result set and its statement. */
    public static ResultSet find(Connection c, String name) throws SQLException {
        PreparedStatement st = c.prepareStatement("SELECT * FROM receipts WHERE name = ?");
        try {
            // Preserve the previous lookup of Java null as the literal name "null".
            st.setString(1, String.valueOf(name));
            return st.executeQuery();
        } catch (SQLException | RuntimeException | Error failure) {
            try {
                st.close();
            } catch (SQLException | RuntimeException | Error cleanupFailure) {
                if (cleanupFailure != failure) {
                    failure.addSuppressed(cleanupFailure);
                }
            }
            throw failure;
        }
    }
}
