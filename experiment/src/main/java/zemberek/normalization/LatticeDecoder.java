package zemberek.normalization;

import com.google.common.math.IntMath;

import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class LatticeDecoder {


    void decode() {

        ActiveList current = ActiveList.builder(10).build();
        ActiveList next = ActiveList.builder(10).build();

    }


    static class InputSequence {
        List<Token> tokens;


    }

    static class Token {
        String word;
        int id;

        public Token(String word, int id) {
            this.word = word;
            this.id = id;
        }
    }

    static class Hyp implements Comparable<Hyp> {
        Hyp previous;
        int[] history = new int[0];
        float cost;

        public float getCost() {
            return cost;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Hyp hyp = (Hyp) o;

            return Arrays.equals(history, hyp.history);
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(history);
        }

        @Override
        public int compareTo(Hyp o) {
            return Float.compare(cost, o.cost);
        }
    }

    static class ActiveList {

        public static float DEFAULT_LOAD_FACTOR = 0.7f;

        public static int DEFAULT_INITIAL_CAPACITY = 16;

        public static int DEFAULT_MIN_HYPOTHESIS_COUNT = 2;

        private Hyp[] hypotheses;
        private int capacity = DEFAULT_INITIAL_CAPACITY;
        private float beamSize;

        private float min = Float.POSITIVE_INFINITY;
        private float max = Float.NEGATIVE_INFINITY;

        private int modulo;
        private int size;
        private int expandLimit;
        private int minHypothesisCount = DEFAULT_MIN_HYPOTHESIS_COUNT;

        private Builder builder;

        public ActiveList(Builder builder) {
            this.beamSize = builder.beamSize;
            this.capacity = equalOrLargerPowerOfTwo(builder.initialCapacity);
            this.hypotheses = new Hyp[capacity];
            this.expandLimit = (int) (DEFAULT_LOAD_FACTOR * capacity);
            this.minHypothesisCount = builder.minimumHypothesisCount;
            this.modulo = capacity - 1;
            // save this builder for cloning.
            this.builder = builder;
        }

        public ActiveList(Builder builder, int newInitialCapacity) {
            this.beamSize = builder.beamSize;
            this.capacity = equalOrLargerPowerOfTwo(newInitialCapacity);
            this.hypotheses = new Hyp[capacity];
            this.expandLimit = (int) (DEFAULT_LOAD_FACTOR * capacity);
            this.minHypothesisCount = builder.minimumHypothesisCount;
            this.modulo = capacity - 1;
            // save this builder for cloning.
            this.builder = builder;
        }

        private ActiveList copyForExpansion() {
            return new ActiveList(builder, capacity * 2);
        }

        public ActiveList newInstance() {
            return new ActiveList(builder);
        }

        public static Builder builder(float beamSize) {
            return new Builder(beamSize);
        }

        public static class Builder {
            float beamSize;
            int initialCapacity = DEFAULT_INITIAL_CAPACITY;
            float loadFactor = DEFAULT_LOAD_FACTOR;
            int minimumHypothesisCount = DEFAULT_MIN_HYPOTHESIS_COUNT;

            public Builder(float beamSize) {
                this.beamSize = beamSize;
            }

            public Builder initialCapacity(int initialCapacity) {
                this.initialCapacity = initialCapacity;
                return this;
            }

            public Builder loadFactor(float loadFactor) {
                if (loadFactor < 0.1 || loadFactor > 0.9) {
                    throw new IllegalArgumentException("Load factor must be between 0.1 and 0.9. But it is " + loadFactor);
                }
                this.loadFactor = loadFactor;
                return this;
            }

            public Builder minimumHypothesisCount(int minimumHypothesisCount) {
                this.minimumHypothesisCount = minimumHypothesisCount;
                return this;
            }

            public ActiveList build() {
                return new ActiveList(this);
            }

        }

        int equalOrLargerPowerOfTwo(int i) {
            return IntMath.isPowerOfTwo(i) ? i : IntMath.pow(2, IntMath.log2(i, RoundingMode.UP));
        }

        // Extra hashing may be necessary. This is similar to Java Map's extra hashing.
        private int firstProbe(int hashCode) {
            return (hashCode ^ ((hashCode << 5) + (hashCode >>> 2))) & modulo;
        }

        private int nextProbe(int previous, int count) {
            return (previous + count) & modulo;
        }

        /**
         * Finds either an empty slot location in Hypotheses array or the location of an equivalent Hypothesis.
         * If an empty slot is found, it returns -(slot index)-1, if an equivalent Hypotheses is found, returns
         * equal hypothesis's slot index.
         */
        private int locate(Hyp hyp) {
            int count = 0;
            int slot = firstProbe(hyp.hashCode());
            while (true) {
                final Hyp h = hypotheses[slot];
                if (h == null) {
                    return (-slot - 1);
                }
                if (h.equals(hyp)) {
                    return slot;
                }
                slot = nextProbe(slot, ++count);
            }
        }

        boolean checkScore(float score) {
            return size < minHypothesisCount || score - min <= beamSize;
        }

        /**
         * Adds a new hypothesis to the list.
         * This method does not check for beam pruning. Therefore, before calling this function, checkScore should be called.
         **/
        public void add(Hyp hypothesis) {

            int slot = locate(hypothesis);

            if (slot < 0) {
                slot = -slot - 1;
                hypotheses[slot] = hypothesis;
                size++;
            } else {
                // Viterbi merge.
                if (hypotheses[slot].getCost() > hypothesis.getCost()) {
                    hypotheses[slot] = hypothesis;
                }
            }
            updateMinMax(hypothesis.getCost());
            if (size == expandLimit) {
                expand();
            }
        }

        private void updateMinMax(float cost) {
            if (min > cost) {
                min = cost;
            }
            if (max < cost) {
                max = cost;
            }
        }

        private void expand() {
            ActiveList expandedList = copyForExpansion();
            // put hypotheses to new list.
            for (int i = 0; i < hypotheses.length; i++) {
                Hyp hyp = hypotheses[i];
                if (hyp == null) {
                    continue;
                }
                int probeCount = 0;
                int slot = firstProbe(hyp.hashCode());
                while (true) {
                    final Hyp h = expandedList.hypotheses[slot];
                    if (h == null) {
                        expandedList.hypotheses[slot] = hyp;
                        break;
                    }
                    slot = nextProbe(slot, ++probeCount);
                }
            }
            this.hypotheses = expandedList.hypotheses;
            this.modulo = expandedList.modulo;
            this.capacity = expandedList.capacity;
            this.expandLimit = expandedList.expandLimit;
        }

        List<Hyp> getAllHypotheses() {
            List<Hyp> result = new ArrayList<>(size + 1);
            for (Hyp hypothesis : hypotheses) {
                if (hypothesis != null) {
                    result.add(hypothesis);
                }
            }
            return result;
        }

        Hyp getBest() {
            Hyp best = null;
            for (Hyp hypothesis : hypotheses) {
                if (hypothesis != null) {
                    if (best == null) {
                        best = hypothesis;
                    } else if (hypothesis.getCost() < best.getCost()) {
                        best = hypothesis;
                    }
                }
            }
            return best;
        }


        List<Hyp> getActiveHypothesesFullySorted() {
            List<Hyp> result = getAllHypotheses();
            Collections.sort(result);
            return result;
        }
    }


}
