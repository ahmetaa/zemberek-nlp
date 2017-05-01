package zemberek.ner;

class NerToken {
    int index;
    String word;
    String normalized;
    String type;
    String tokenId;
    NePosition position;

    public NerToken(int index, String word, String normalized, String type, NePosition position) {
        this.index = index;
        this.word = word;
        this.normalized = normalized;
        this.type = type;
        this.position = position;
        this.tokenId = getTokeId();
    }

    public NerToken(int index, String word, String type, NePosition position) {
        this.index = index;
        this.word = word;
        this.normalized = word;
        this.type = type;
        this.position = position;
        this.tokenId = getTokeId();
    }

    static NerToken fromTypePositionString(int index, String word, String normalized, String id) {
        if (id.equals("O")) {
            return new NerToken(index, word, normalized, NerDataSet.OUT_TOKEN_TYPE, NePosition.OUTSIDE);
        }
        if (!id.contains("_")) {
            throw new IllegalStateException("Id value should contain _ but : " + id);
        }
        int p = id.indexOf('_');
        String type = id.substring(0, p);
        NePosition pos = NePosition.fromString(id.substring(p + 1));
        return new NerToken(index, word, normalized, type, pos);
    }

    private String getTokeId() {
        if (position == NePosition.OUTSIDE) {
            return "O";
        } else {
            return type + "_" + position.shortForm;
        }
    }


    @Override
    public String toString() {
        return "[" +
                +index +
                ", " + word +
                ", " + normalized +
                ", " + type +
                ", " + position +
                ']';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        NerToken token = (NerToken) o;

        if (index != token.index) return false;
        if (!word.equals(token.word)) return false;
        if (!normalized.equals(token.normalized)) return false;
        return tokenId.equals(token.tokenId);
    }

    @Override
    public int hashCode() {
        int result = index;
        result = 31 * result + word.hashCode();
        result = 31 * result + normalized.hashCode();
        result = 31 * result + tokenId.hashCode();
        return result;
    }
}
