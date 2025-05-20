import java.util.Comparator;

/**
 * @author ericfouh
 */
public interface ITerm
        extends Comparable<ITerm> {

    /**
     * Compares the two terms in descending order by weight.
     *
     * @return comparator Object
     */
    public static Comparator<ITerm> byReverseWeightOrder() {
        return new Comparator<ITerm>() {
            @Override
            public int compare(ITerm o1, ITerm o2) {
                if (o1.getWeight() < o2.getWeight()) {
                    return 1;
                } else if (o1.getWeight() > o2.getWeight()) {
                    return -1;
                } else {
                    return o1.compareTo(o2);
                }
            }
        };
    }

    /**
     * Compares the two terms in lexicographic order but using only the first r
     * characters of each query.
     *
     * @param r
     * @return comparator Object
     */
    public static Comparator<ITerm> byPrefixOrder(int r) {
        if (r < 0) {
            throw new IllegalArgumentException("r should not be negative");
        }
        return new Comparator<ITerm>() {
            @Override
            public int compare(ITerm term1, ITerm term2) {
                String query1 = term1.getTerm();
                String query2 = term2.getTerm();
                for (int i = 0; i < Math.min(r, Math.min(query1.length(), query2.length())); i++) {
                    if (query1.charAt(i) != query2.charAt(i)) {
                        return Character.compare(query1.charAt(i), query2.charAt(i));
                    }
                }
                if (query1.length() < query2.length() && query1.length() < r) {
                    return -1;
                } else if (query2.length() < query1.length() && query2.length() < r) {
                    return 1;
                } else {
                    return 0;
                }
            }
        };
    }

    // Compares the two terms in lexicographic order by query.
    public int compareTo(ITerm that);


    // Returns a string representation of this term in the following format:
    // the weight, followed by a tab, followed by the query.
    public String toString();

    // Required getters.
    public long getWeight();
    public String getTerm();

    // Required setters (mostly for autograding purposes)
    public void setWeight(long weight);
    public String setTerm(String term);

}
