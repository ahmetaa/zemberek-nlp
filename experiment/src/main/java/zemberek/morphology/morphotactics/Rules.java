package zemberek.morphology.morphotactics;

import java.util.Arrays;
import java.util.List;

public class Rules {

    public static Rule allowOnly(String key) {
        return new AllowOnly(key);
    }

    public static Rule allowAny(String... keys) {
        return new AllowAny(keys);
    }

    public static Rule mandatory(String key) {
        return new Mandatory(key);
    }

    public static Rule rejectOnly(String key) {
        return new RejectOnly(key);
    }

    public static Rule rejectAny(String... keys) {
        return new RejectAny(keys);
    }

    public static class AllowOnly implements Rule {

        String key;

        public AllowOnly(String key) {
            this.key = key;
        }

        @Override
        public boolean canPass(GraphVisitor visitor) {
            return visitor.containsKey(key);
        }
    }

    public static class Mandatory implements Rule {

        String key;

        public Mandatory(String key) {
            this.key = key;
        }

        @Override
        public boolean canPass(GraphVisitor visitor) {
            return visitor.containsKey(key);
        }
    }

    public static class AllowAny implements Rule {

        List<String> keys;

        public AllowAny(String... keys) {
            this.keys = Arrays.asList(keys);
        }

        @Override
        public boolean canPass(GraphVisitor visitor) {
            for (String key : keys) {
                if (visitor.containsKey(key)) {
                    return true;
                }
            }
            return false;
        }
    }

    public static class RejectOnly implements Rule {

        String key;

        RejectOnly(String key) {
            this.key = key;
        }

        @Override
        public boolean canPass(GraphVisitor visitor) {
            return !visitor.containsKey(key);
        }
    }

    public static class RejectAny implements Rule {

        List<String> keys;

        RejectAny(String... keys) {
            this.keys = Arrays.asList(keys);
        }

        @Override
        public boolean canPass(GraphVisitor visitor) {
            for (String key : keys) {
                if (visitor.containsKey(key)) {
                    return false;
                }
            }
            return true;
        }
    }

}
