package zemberek.morphology.analysis.tr;

/**
 * Converts a number from text form to digit form for turkish
 */
public class TurkishTextToNumberConverter {

    private enum State {
        START,
        ST_ONES_1, ST_ONES_2, ST_ONES_3, ST_ONES_4, ST_ONES_5, ST_ONES_6,
        ST_TENS_1, ST_TENS_2, ST_TENS_3,
        ST_HUNDREDS_1, ST_HUNDREDS_2,
        ST_THOUSAND,
        END,
        ERROR
    }

    private enum TransitionType {
        T_ZERO,
        T_ONE,
        T_TWO_TO_NINE,
        T_TENS,
        T_HUNDRED,
        T_THOUSAND,
        T_MILLION,
        T_BILLION,
        T_TRILLION,
        T_QUADRILLION;

        public static TransitionType getTypeByValue(long number) {
            switch (digitCount(number)) {
                case 1:
                    if (number == 1)
                        return T_ONE;
                    else if (number == 0)
                        return T_ZERO;
                    else
                        return T_TWO_TO_NINE;
                case 2:
                    return T_TENS;
                case 3:
                    return T_HUNDRED;
                case 4:
                    return T_THOUSAND;
                case 7:
                    return T_MILLION;
                case 10:
                    return T_BILLION;
                case 13:
                    return T_TRILLION;
                case 16:
                    return T_QUADRILLION;
                default:
                    throw new IllegalArgumentException("cannot create a Transition from value: " + number);
            }
        }
    }

    private static int digitCount(long num) {
        int i = 0;
        do {
            num = num / 10;
            i++;
        } while (num > 0);
        return i;
    }

    private class Transition {
        final long value;
        final TransitionType type;

        public Transition(String str) {
            this.value = TurkishNumbers.singleWordNumberValue(str);
            this.type = TransitionType.getTypeByValue(this.value);
        }

        public String toString() {
            return "[" + type.name() + ":" + value + "]";
        }
    }

    /**
     * Converts an array of digit text Strings to a long number.
     * <p>Example:
     * <p>[on,iki] returns 12
     * <p>[bin,on,iki] returns 1012
     * <p>[seksen,iki,milyon,iki] returns 82000002
     *
     * @param words digit string array
     * @return number equivalent of the word array, or -1 if word array is parsable like [bir,bin] [milyon] [on,bir,iki]
     */
    public long convert(String... words) {
        State state = State.START;
        for (String word : words) {
            state = acceptTransition(state, new Transition(word));
            if (state == State.ERROR)
                return -1;
        }
        return valueToAdd + total;
    }

    private long total;
    private long valueToAdd;
    private Transition previousMil = new Transition(TurkishNumbers.SIFIR);

    private State acceptTransition(State currentState, Transition transition) {

        switch (transition.type) {

            case T_ZERO:
                switch (currentState) {
                    case START:
                        return total == 0 ? State.END : State.ERROR;
                }
                break;

            case T_ONE:
                switch (currentState) {
                    case START:
                        add(1);
                        return State.ST_ONES_1;
                    case ST_TENS_1:
                        add(1);
                        return State.ST_ONES_3;
                    case ST_TENS_2:
                    case ST_HUNDREDS_1:
                        add(1);
                        return State.ST_ONES_4;
                    case ST_TENS_3:
                    case ST_HUNDREDS_2:
                    case ST_THOUSAND:
                        add(1);
                        return State.ST_ONES_5;
                }
                break;

            case T_TWO_TO_NINE:
                switch (currentState) {
                    case START:
                        add(transition.value);
                        return State.ST_ONES_2;
                    case ST_TENS_1:
                        add(transition.value);
                        return State.ST_ONES_3;
                    case ST_TENS_2:
                    case ST_HUNDREDS_1:
                        add(transition.value);
                        return State.ST_ONES_4;
                    case ST_TENS_3:
                    case ST_HUNDREDS_2:
                        add(transition.value);
                        return State.ST_ONES_5;
                    case ST_THOUSAND:
                        add(transition.value);
                        return State.ST_ONES_6;
                }
                break;

            case T_TENS:
                switch (currentState) {
                    case START:
                        add(transition.value);
                        return State.ST_TENS_1;
                    case ST_HUNDREDS_1:
                        add(transition.value);
                        return State.ST_TENS_2;
                    case ST_HUNDREDS_2:
                    case ST_THOUSAND:
                        add(transition.value);
                        return State.ST_TENS_3;
                }
                break;

            case T_HUNDRED:
                switch (currentState) {
                    case START:
                    case ST_ONES_2:
                        mul(100);
                        return State.ST_HUNDREDS_1;
                    case ST_THOUSAND:
                    case ST_ONES_6:
                        mul(100);
                        return State.ST_HUNDREDS_2;
                }
                break;

            case T_THOUSAND:
                switch (currentState) {
                    case START:
                    case ST_ONES_2:
                    case ST_ONES_3:
                    case ST_ONES_4:
                    case ST_TENS_1:
                    case ST_TENS_2:
                    case ST_HUNDREDS_1:
                        addToTotal(transition.value);
                        return State.ST_THOUSAND;
                }
                break;

            case T_MILLION:
            case T_BILLION:
            case T_TRILLION:
            case T_QUADRILLION:
                switch (currentState) {
                    case ST_ONES_1:
                    case ST_ONES_2:
                    case ST_ONES_3:
                    case ST_ONES_4:
                    case ST_TENS_1:
                    case ST_TENS_2:
                    case ST_HUNDREDS_1:
                        // millions, billions etc behaves the same.
                        // here we prevent "billion" comes after a "million"
                        // for this, we remember the last big number in previousMil variable..
                        if (previousMil.value == 0 || previousMil.value > transition.value) {
                            previousMil = transition;
                            addToTotal(transition.value);
                            return State.START;
                        } else return State.ERROR;
                }

        }
        return State.ERROR;
    }

    private void add(long val) {
        valueToAdd += val;
    }

    private void mul(long val) {
        if (valueToAdd == 0)
            valueToAdd = val;
        else
            valueToAdd = valueToAdd * val;
    }

    private void addToTotal(long val) {
        if (valueToAdd == 0)
            total += val;
        else
            total += valueToAdd * val;
        valueToAdd = 0;
    }
}
