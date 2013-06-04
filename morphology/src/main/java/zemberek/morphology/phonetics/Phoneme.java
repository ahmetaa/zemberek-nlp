package zemberek.morphology.phonetics;

public class Phoneme {
    public final String id;
    public final String asciiId;
    public final String surfaceForm;
    public final PhoneticSound phoneticSound;

    public static final Phoneme PAUSE = new Phoneme("pau", "pau", "pau", PhoneticSound.UNDEFINED);
    public static final Phoneme EMPTY = new Phoneme("x", "x", "empty", PhoneticSound.UNDEFINED);

    public Phoneme(String id, String asciiId, String surfaceForm, PhoneticSound phoneticSound) {
        this.id = id;
        this.asciiId = asciiId;
        this.surfaceForm = surfaceForm;
        this.phoneticSound = phoneticSound;
    }

    @Override
    public String toString() {
        return id;
    }

    public String asText(PhoneticRepresentationFormat format) {
        switch (format) {
            case CUSTOM:
                return id;
            case CUSTOM_ASCII:
                return asciiId;
            case IPA_UNICODE:
                return phoneticSound.unicode;
            case XSAMPA:
                return phoneticSound.xSampa;
        }
        throw new RuntimeException("Cannot be here..");
    }

    public String getId() {
        return id;
    }

    public String getSurfaceForm() {
        return surfaceForm;
    }

    public boolean isVowel() {
        return phoneticSound.vowel;
    }

    public String getIpaUnicode() {
        return phoneticSound.unicode;
    }

    public String getXSampa() {
        return phoneticSound.xSampa;
    }


}
