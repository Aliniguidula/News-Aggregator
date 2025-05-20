public class Term implements ITerm {

    private String term;
    private long weight;

    /**
     * Initialize a Term with a given query String and weight
     */
    public Term(String term, long weight) {
        if (term == null) {
            throw new IllegalArgumentException("Query cannot be null");
        }
        if (weight < 0) {
            throw new IllegalArgumentException("Weight cannot be negative");
        }
        this.term = term;
        this.weight = weight;
    }


    @Override
    public int compareTo(ITerm that) {
        return this.term.compareTo(that.getTerm());
    }

    @Override
    public String toString() {
        return weight + "\t" + term;
    }

    @Override
    public long getWeight() {
        return this.weight;
    }

    @Override
    public String getTerm() {
        return this.term;
    }

    @Override
    public void setWeight(long weight) {
        if (weight < 0) {
            throw new IllegalArgumentException("Weight cannot be negative");
        }
        this.weight = weight;
    }

    @Override
    public String setTerm(String term) {
        if (term == null) {
            throw new IllegalArgumentException("Term cannot be null");
        }
        return this.term = term;
    }
}
