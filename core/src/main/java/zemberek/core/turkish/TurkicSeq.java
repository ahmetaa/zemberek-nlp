package zemberek.core.turkish;

import java.util.Arrays;

/**
 * <code>TurkicSeq</code> represents a sequence of <code>Letter</code>s. It is mutable and not Thread safe.
 * should be used for intermediate operations, not as storage.
 */
public class TurkicSeq implements CharSequence, Comparable<TurkicSeq> {

    private TurkicLetter[] letters;
    private int size = 0;
    public static final TurkicSeq EMPTY_SEQ = new TurkicSeq(0);

    /**
     * default constructor. Creates an array with  7 letter reference.
     */
    public TurkicSeq() {
        letters = new TurkicLetter[7];
    }

    /**
     * 'capacity' boyutlu 'Letter' dizisine sahip nesne olusturur.
     *
     * @param capacity baslangic olusan TurkicLetter[] boyu
     */
    public TurkicSeq(int capacity) {
        letters = new TurkicLetter[capacity];
    }

    /**
     * 'capacity' boyutlu 'Letter' dizisine sahip nesne olusturur. daha sonra
     * girisi String'i icindeki karakterleri TurkicLetter seklinde TurkicLetter dizisine aktarir.
     * Eger String boyu capacityden buyukse capacity'yi boy'a esitler.
     * Eger String icindeki karakter Alfabe'de yar almiyorsa "TANIMSIZ_HARF" harfi olarak eklenir.
     *
     * @param str      ornek alincak String
     * @param capacity baslangic olusan TurkicLetter[] boyu
     * @param alphabet ilgili alphabet
     */
    public TurkicSeq(String str, TurkishAlphabet alphabet, int capacity) {
        if (capacity < str.length())
            capacity = str.length();
        letters = new TurkicLetter[capacity];
        size = str.length();
        for (int i = 0; i < size; i++)
            letters[i] = alphabet.getLetter(str.charAt(i));
    }


    /**
     * Belirlenen alphabet ile String icerigini Harflere donusturur.
     *
     * @param str      ornek alincak String
     * @param alphabet ilgili alphabet
     */
    public TurkicSeq(String str, TurkishAlphabet alphabet) {
        size = str.length();
        letters = new TurkicLetter[size];
        for (int i = 0; i < size; i++)
            letters[i] = alphabet.getLetter(str.charAt(i));
    }

    /**
     * Copy-Constructor. gelen harf dizisi ile ayni icerige sahip olacak sekilde
     * TurkicLetter dizisi olusturur.
     *
     * @param sequence ornek alinacak TurkicSeq
     */
    public TurkicSeq(TurkicSeq sequence) {
        size = sequence.length();
        letters = new TurkicLetter[size];
        System.arraycopy(sequence.letters, 0, letters, 0, size);
    }

    /**
     * gelen TurkicLetter dizisini icerige kopyalar.
     *
     * @param sequence kopyalancak TurkicLetter dizisi.
     */
    private TurkicSeq(TurkicLetter[] sequence) {
        size = sequence.length;
        letters = new TurkicLetter[size];
        System.arraycopy(sequence, 0, letters, 0, size);
    }

    /**
     * bu metod harf referansi dizisini serbest birakmaz,
     * sadece boyu sifira indirir.
     */
    public void erase() {
        size = 0;
    }

    /**
     * returns the last letter.
     *
     * @return last letter.
     * @throws IllegalStateException if there is no letter in sequence.
     */
    public TurkicLetter lastLetter() {
        if (size < 1)
            throw new IllegalStateException("Letter sequence is empty");
        return letters[size - 1];
    }

    /**
     * returns the last vowel.
     *
     * @return last vowel.
     * @throws IllegalStateException if there is no vowel in sequence.
     */
    public TurkicLetter lastVowel() {
        for (int i = size - 1; i >= 0; i--) {
            if (letters[i].isVowel())
                return letters[i];
        }
        throw new IllegalStateException("There is no wovel in the sequence:" + Arrays.toString(letters));
    }

    /**
     * ic metod. harf dizisinin boyutu yetersiz geldiginde "ek" miktarinda daha
     * fazla yere sahip yeni dizi olusturulup icerik yeni diziye kopyalanir.
     *
     * @param ek eklenecek TurkicSeq miktari.
     */
    private void adjustCapacity(int ek) {
        TurkicLetter[] newSeq = new TurkicLetter[letters.length + ek];
        System.arraycopy(letters, 0, newSeq, 0, letters.length);
        letters = newSeq;
    }

    /**
     * otomatik capacity ayarlama. dizi boyu iki katina cikarilir.
     */
    private void adjustCapacity() {
        TurkicLetter[] newSeq = new TurkicLetter[letters.length * 2];
        System.arraycopy(letters, 0, newSeq, 0, letters.length);
        letters = newSeq;
    }

    /**
     * kelimenin sonuna harf ekler.
     *
     * @param harf eklenecek harf
     * @return this
     */
    public TurkicSeq append(TurkicLetter harf) {
        if (size == letters.length)
            adjustCapacity(3);
        letters[size++] = harf;
        return this;
    }

    /**
     * girilen pozisyona herf ekler, bu noktadan sonraki harfler otelenir.
     * "armut" icin (2, a) "aramut" uretir.
     *
     * @param index eklenecek pozisyon
     * @param harf  eklenecek harf.
     * @throws ArrayIndexOutOfBoundsException
     */
    public void insert(int index, TurkicLetter harf) {
        if (index < 0 || index > size)
            throw new ArrayIndexOutOfBoundsException("index degeri:" + index + " fakat harf dizi boyu:" + size);

        if (size == letters.length)
            adjustCapacity();

        for (int i = size - 1; i >= index; i--)
            letters[i + 1] = letters[i];
        letters[index] = harf;
        size++;
    }

    /**
     * Diziye baska bir harf dizisinin icerigini ular.
     *
     * @param hdizi ulanacak harf dizisi.
     * @return this.
     */
    public TurkicSeq append(TurkicSeq hdizi) {
        int hboy = hdizi.length();
        if (size + hboy > letters.length)
            adjustCapacity(hboy);

        System.arraycopy(hdizi.letters, 0, letters, size, hboy);
        size += hdizi.length();
        return this;
    }

    /**
     * Diziye baska bir harf dizisinin icerigini index ile belirtilen harften itibaren ekler.
     * "armut" icin (2, hede) "arhedemut" uretir.
     *
     * @param index    eklencek pozisyon
     * @param sequence eklenecek harf dizisi
     * @return this.
     * @throws ArrayIndexOutOfBoundsException
     */
    public TurkicSeq append(int index, TurkicSeq sequence) {
        if (index < 0 || index > size)
            throw new ArrayIndexOutOfBoundsException("indeks degeri:" + index + " fakat harf dizi boyu:" + size);

        //dizi capacitysini ayarla
        int seqSize = sequence.length();
        if (size + seqSize > letters.length)
            adjustCapacity(seqSize);

        //sondan baslayarak this.dizinin index'ten sonraki kismini dizinin sonuna tasi
        for (int i = seqSize + size - 1; i >= seqSize; i--)
            letters[i] = letters[i - seqSize];

        //gelen diziyi kopyala ve boyutu degistir.
        System.arraycopy(sequence.letters, 0, letters, index, seqSize);
        size += sequence.length();
        return this;
    }

    public TurkicSeq araDizi(int bas, int son) {
        if (son < bas) return null;
        TurkicLetter[] yeniHarfler = new TurkicLetter[son - bas];
        System.arraycopy(letters, bas, yeniHarfler, 0, son - bas);
        return new TurkicSeq(yeniHarfler);
    }

    /**
     * verilen pozisyondaki harfi dondurur. icerigi "kedi" olan TurkicSeq icin
     * harf(1) e dondurur.
     *
     * @param i TurkicLetter index.
     * @return TurkicLetter from given index i.
     * @throws IndexOutOfBoundsException if <code>i</code> is smaller than zero or larger than size-1
     */
    public TurkicLetter getLetter(int i) {
        if (i >= size || i < 0)
            throw new IndexOutOfBoundsException("Cannot retrieve letter from sequence with index:" + i);
        return letters[i];
    }

    /**
     * ilk sesliyi dondurur. eger sesli yoksa UNDEFINED_LETTER doner. aramaya belirtilen indeksten baslar.
     *
     * @param index baslangic indeksi.
     * @return varsa ilk sesli, yoksa UNDEFINED_LETTER
     */
    public TurkicLetter firstVowel(int index) {
        for (int i = index; i < size; i++) {
            if (letters[i].isVowel())
                return letters[i];
        }
        throw new IllegalStateException("Letter sequence is already empty");
    }


    /**
     * returns the first vowel.
     *
     * @return first vowel.
     * @throws IllegalStateException if there is no vowel in sequence.
     */
    public TurkicLetter firstVowel() {
        for (int i = 0; i < size; i++) {
            if (letters[i].isVowel())
                return letters[i];
        }
        throw new IllegalStateException("There is no wovel in the sequence:" + Arrays.toString(letters));
    }

    public boolean hasVowel() {
        for (int i = 0; i < size; i++) {
            if (letters[i].isVowel())
                return true;
        }
        return false;
    }

    /**
     * Tam esitlik kiyaslamasi. kiyaslama nesne tipi, ardindan da TurkicLetter dizisi icindeki
     * harflerin char iceriklerine gore yapilir.
     *
     * @param o kiyaslanacak nesne
     * @return true eger esitse.
     */
    public boolean equals(Object o) {
        if (o == null) return false;
        if (this == o) return true;
        if (!(o instanceof TurkicSeq)) return false;

        final TurkicSeq harfDizisi = (TurkicSeq) o;
        if (size != harfDizisi.size) return false;
        for (int i = 0; i < size; i++) {
            if (letters[i].charValue() != harfDizisi.letters[i].charValue())
                return false;
        }
        return true;
    }

    public boolean startsWith(TurkicSeq ls) {
        if (ls.size > size)
            return false;
        for (int i = 0; i < ls.letters.length; i++) {
            if (!letters[i].equals(ls.letters[i]))
                return false;
        }
        return true;
    }

    public int hashCode() {
        return toString().hashCode();
    }

    /**
     * istenen noktadaki harfi giris parametresi olan TurkicLetter ile degistirir.
     *
     * @param index  degistirilecek indeks.
     * @param letter kullanilacak harf
     * @throws ArrayIndexOutOfBoundsException
     */
    public void changeLetter(int index, TurkicLetter letter) {
        validateIndex(index);
        letters[index] = letter;
    }

    /**
     * istenen noktadaki harfi giris parametresi olan TurkicLetter ile degistirir.
     *
     * @param letter kullanilacak harf
     * @throws ArrayIndexOutOfBoundsException
     */
    public void changeLast(TurkicLetter letter) {
        if (size < 1)
            throw new IllegalStateException("Letter sequence is empty");
        letters[size - 1] = letter;
    }

    private void validateIndex(int index) {
        if (index < 0 || index >= size)
            throw new ArrayIndexOutOfBoundsException("index value is:" + index + " But sequence length is:" + size);
    }

    /**
     * son harfi siler. eger harf yoksa hicbir etki yapmaz.
     */
    public TurkicSeq eraseLast() {
        if (size > 0)
            size--;
        return this;
    }

    /**
     * verilen pozisyondaki harfi siler. kelimenin kalan kismi otelenir.
     * eger verilen pozisyon yanlis ise  ArrayIndexOutOfBoundsException firlatir.
     * <p/>
     * "kedi" icin (2) "kei" olusturur.
     *
     * @param index silinecek harf pozisyonu
     * @return dizinin kendisi.
     * @throws ArrayIndexOutOfBoundsException
     */
    public TurkicSeq delete(int index) {
        validateIndex(index);
        if (index == size - 1) {
            size--;
        } else {
            System.arraycopy(letters, index + 1, letters, index, size - index - 1);
            size--;
        }
        return this;
    }

    /**
     * verilen pozisyondan belli miktar harfi siler.
     * "kediler" icin (2,2) "keler" olusturur.
     *
     * @param index      silinmeye baslanacak pozisyon
     * @param harfSayisi silinecek harf miktari
     * @return dizinin kendisi
     */
    public TurkicSeq delete(int index, int harfSayisi) {
        validateIndex(index);
        if (index + harfSayisi > size)
            harfSayisi = size - index;
        for (int i = index + harfSayisi; i < size; i++)
            letters[i - harfSayisi] = letters[i];
        size -= harfSayisi;
        return this;
    }

    /**
     * ilk harfi dondurur. eger harf yoksa UNDEFINED_LETTER doner.
     *
     * @return ilk TurkicLetter.
     */
    public TurkicLetter firstLetter() {
        if (size == 0)
            throw new IllegalStateException("Letter sequence is empty");
        return letters[0];
    }

    /**
     * "index" numarali harften itibaren siler.
     * "kedi" icin (1) "k" olusturur.
     *
     * @param index kirpilmaya baslanacak pozisyon
     */
    public void clip(int index) {
        if (index <= size && index >= 0)
            size = index;
    }

    /**
     * sadece belirli bir bolumunu String'e donusturur.
     *
     * @param index String'e donusum baslangic noktasi.
     * @return olusan String.
     */
    public String toString(int index) {
        if (index < 0 || index >= size) return "";
        StringBuilder s = new StringBuilder(size - index);
        for (int i = index; i < size; i++)
            s.append(charAt(i));
        return s.toString();
    }

    @Override
    public String toString() {
        return new StringBuilder(this).toString();
    }

    /**
     * Compare to metodu siralama icin kiyaslama yapar. Kiyaslama oncelikle harflerin alphabettik sirasina
     * daha sonra dizilerin boyutuna gore yapilir.
     *
     * @param o kiyaslanacak dizi.
     * @return 'kedi'.compareTo('kedi') -> 0
     *         'kedi'.compareTo('ke')  -> 2 (boy farki)
     *         'kedi'.compareTo('kedm') -> -4 (i->m alphabettik sira farki)
     *         'kedi'.compareTo(null) -> 1
     */
    public int compareTo(TurkicSeq o) {
        if (o == null)
            return 1;

        if (this == o)
            return 0;

        int l = o.size;
        int n = Math.min(size, l);

        for (int i = 0; i < n; i++) {
            if (!letters[i].equals(o.letters[i]))
                return letters[i].alphabeticIndex() - o.letters[i].alphabeticIndex();
        }
        return size - l;
    }

    /* ------------------------- ozel metodlar ------------------------------- */

    /**
     * Genellikle kelimedeki hece sayisini bulmak icin kullanilir.
     *
     * @return inte, sesli harf sayisi.
     */
    public int vowelCount() {
        int sonuc = 0;
        for (int i = 0; i < size; i++) {
            if (letters[i].isVowel())
                sonuc++;
        }
        return sonuc;
    }

    /**
     * @return hepsi buyuk harf ise true, boy=0 dahil.
     */
    public boolean isAllCapital() {
        for (int i = 0; i < size; i++) {
            if (Character.isLowerCase(letters[i].charValue()))
                return false;
        }
        return true;
    }

    //--------- CharSequence methods -----

    public int length() {
        return size;
    }

    public char charAt(int index) {
        if (index < 0 || index >= size)
            throw new StringIndexOutOfBoundsException(index);
        return letters[index].charValue();
    }

    public TurkicSeq subSequence(int start, int end) {
        return araDizi(start, end);
    }

}
