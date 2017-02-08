package zemberek.morphology.morphotactics;

public class Attributes {
    
    public static Attribute hasMorpheme(Morpheme morpheme) {
        return new HasMorpheme(morpheme);
    }

    public static class HasMorpheme implements Attribute<Morpheme> {
        Morpheme morpheme;

        HasMorpheme(Morpheme morpheme) {
            this.morpheme = morpheme;
        }

        @Override
        public boolean check(Morpheme morpheme) {
            return this.morpheme.equals(morpheme);
        }
    }
}
