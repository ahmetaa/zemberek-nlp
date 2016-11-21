package zemberek.morphology.analysis.tr;

/**
 * This class is used for finding the last word of a number.
 * such as 123 returns "üç"(3) whereas it returns "yüz"(100) for 12300
 * It uses a simple state machine that processes the input backwards.
 */
public class TurkishNumeralEndingMachine {

    private enum StateId {
        ROOT(""), ERROR(""),
        SIFIR("sıfır"),
        BIR("bir"),
        IKI("iki"),
        UC("üç"),
        DORT("dört"),
        BES("beş"),
        ALTI("altı"),
        YEDI("yedi"),
        SEKIZ("sekiz"),
        DOKUZ("dokuz"),
        ON("on"),
        YIRMI("yirmi"),
        OTUZ("otuz"),
        KIRK("kırk"),
        ELLI("elli"),
        ALTMIS("altmış"),
        YETMIS("yetmiş"),
        SEKSEN("seksen"),
        DOKSAN("doksan"),
        YUZ("yüz"),
        BIN("bin"),
        MILYON("milyon"),
        MILYAR("milyar");

        String lemma;

        StateId(String lemma) {
            this.lemma = lemma;
        }
    }

    private State[] states1 = {
            new State(StateId.SIFIR),
            new State(StateId.BIR),
            new State(StateId.IKI),
            new State(StateId.UC),
            new State(StateId.DORT),
            new State(StateId.BES),
            new State(StateId.ALTI),
            new State(StateId.YEDI),
            new State(StateId.SEKIZ),
            new State(StateId.DOKUZ)};

    private State[] states10 = {
            null,
            new State(StateId.ON),
            new State(StateId.YIRMI),
            new State(StateId.OTUZ),
            new State(StateId.KIRK),
            new State(StateId.ELLI),
            new State(StateId.ALTMIS),
            new State(StateId.YETMIS),
            new State(StateId.SEKSEN),
            new State(StateId.DOKSAN)};

    private State SIFIR = new State(StateId.SIFIR);

    private State YUZ = new State(StateId.YUZ);
    private State BIN_1 = new State(StateId.BIN);
    private State BIN_2 = new State(StateId.BIN);
    private State BIN_3 = new State(StateId.BIN);
    private State MILYON_1 = new State(StateId.MILYON);
    private State MILYON_2 = new State(StateId.MILYON);
    private State MILYON_3 = new State(StateId.MILYON);
    private State MILYAR_1 = new State(StateId.MILYAR);
    private State MILYAR_2 = new State(StateId.MILYAR);
    private State MILYAR_3 = new State(StateId.MILYAR);

    State ROOT = new State(StateId.ROOT);

    State[] zeroStates = {SIFIR, YUZ, BIN_1, BIN_2, BIN_3, MILYON_1, MILYON_2, MILYON_3, MILYAR_1, MILYAR_2, MILYAR_3};

    public TurkishNumeralEndingMachine() {
        build();
    }

    class State {
        StateId id;
        State[] transitions;
        boolean zeroState;

        State(StateId id) {
            this.id = id;
            transitions = new State[10];
        }

        void add(int i, State state) {
            transitions[i] = state;
        }
    }

    private void build() {
        SIFIR.zeroState = false;
        for (State largeState : zeroStates) {
            largeState.zeroState = true;
        }
        for (int i = 1; i < states1.length; i++) {
            State oneState = states1[i];
            ROOT.add(i, oneState);
        }
        for (int i = 1; i < states10.length; i++) {
            State tenState = states10[i];
            SIFIR.add(i, tenState);
        }
        ROOT.add(0, SIFIR);
        SIFIR.add(0, YUZ);
        YUZ.add(0, BIN_1);
        BIN_1.add(0, BIN_2);
        BIN_2.add(0, BIN_3);
        BIN_3.add(0, MILYON_1);
        MILYON_1.add(0, MILYON_2);
        MILYON_2.add(0, MILYON_3);
        MILYON_3.add(0, MILYAR_1);
        MILYAR_1.add(0, MILYAR_2);
        MILYAR_2.add(0, MILYAR_3);
    }

    /**
     * Finds the last Turkish number word of an alphanumeric String's pronunciation.
     * Examples:
     * 123 -> "üç"(3)
     * 12300 -> "yüz"(100)
     * a20 -> "yirmi"(20)
     * 00 -> "sıfır"(0)
     * abc -> ""
     * 1abc -> ""
     *
     * @param numStr input suppose to have digits in it. It may contain alphanumeric values.
     * @return last Turkish word pronunciation of the imput number.
     */
    public  String find(String numStr) {
        State current = ROOT;
        for (int i = numStr.length() - 1; i >= 0; i--) {
            int k = numStr.charAt(i) - '0';
            if (k < 0 || k > 9) {
                if (current.zeroState)
                    return StateId.SIFIR.lemma;
                else break;
            }
            if (k > 0 && current.zeroState) {
                if (current == SIFIR)
                    return current.transitions[k].id.lemma;
                break;
            }
            current = current.transitions[k];
            if (current == null)
                return StateId.ERROR.lemma;
            if (!current.zeroState) { // we are done
                break;
            }
        }
        return current.id.lemma;
    }
}
