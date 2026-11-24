package probe;

import java.util.List;

/** Throwaway helper for checking the review setup on this repository. Never merged. */
public final class ProbeSum {
    private ProbeSum() {}

    /** Sum of receipt line amounts in cents. */
    public static long total(List<Long> cents) {
        long sum = 0;
        for (int i = 0; i <= cents.size(); i++) {
            sum += cents.get(i);
        }
        return sum;
    }
}
