package org.blueskiron.goldilocks.api.statemachine;

public interface Sequenced extends Comparable<Sequenced> {

    /**
     * Deafult inital value of Sequenced has index set to -1 and always return null for object
     * entry.
     */
    public static final Sequenced INITIAL = new Initial();

    /**
     * get corresponding index of this sequenced.
     * @return
     */
    public long getIndex();

    /**
     * get corresponding object entry of this sequenced.
     * @return
     */
    public Object getEntry();

    @Override
    public default int compareTo(Sequenced other) {
        return this.getIndex() > other.getIndex() ? +1 : this.getIndex() < other.getIndex() ? -1
                : 0;
    }

    /**
     * Utility class which probably does not belong here.
     * @author jzachar
     * @param <T>
     */
    final class Range {
        private static final String STR_FORMAT = "Range[first=%s, last=%s]";
        private final Sequenced first;
        private final Sequenced last;

        public Range(Sequenced first, Sequenced last) {
            this.first = first;
            this.last = last;
        }

        public Sequenced getFirst() {
            return first;
        }

        public Sequenced getLast() {
            return last;
        }

        @Override
        public String toString() {
//            long fi = -1;
//            long li = -1;
//            if (first != null) {
//                fi = first.getIndex();
//            }
//            if (last != null) {
//                li = last.getIndex();
//            }
            return String.format(STR_FORMAT, first, last);
        }
    }

    class Initial implements Sequenced {

        @Override
        public long getIndex() {
            return -1;
        }

        @Override
        public Object getEntry() {
            return null;
        }

    }
}
