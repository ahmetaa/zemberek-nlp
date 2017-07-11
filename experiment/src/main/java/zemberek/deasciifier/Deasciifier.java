package zemberek.deasciifier;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Scanner;

/**
 * This class provides functionality to deasciify a given ASCII based Turkish
 * text.
 * <p>
 * <p>
 * Note: Adapted from Emre Sevinc's Turkish deasciifier for Python which was
 * influenced from Deniz Yuret's Emacs Turkish Mode implementation which is was
 * inspired by Gokhan Tur's Turkish Text Deasciifier.
 * </p>
 * <p>
 * <p>
 * See: <a
 * href="http://denizyuret.blogspot.com/2006/11/emacs-turkish-mode.html">Deniz
 * Yuret's Emacs Turkish Mode</a><br />
 * <a href="http://ileriseviye.org/blog/?p=3274">Turkish Deasciifier on Emre
 * Sevinc's Blog</a><br />
 * <a href="http://github.com/emres/turkish-deasciifier/">Turkish Deasciifier
 * for Python on Emre Sevinc's Github Repo</a><br />
 * </p>
 * <p>
 * <p>
 * <h3>Usage</h3>
 * <p>
 * <pre>
 * Deasciifier d = new Deasciifier();
 * d.setAsciiString(&quot;Hadi bir masal uyduralim, icinde mutlu, doygun, telassiz durdugumuz.&quot;);
 * System.out.println(d.convertToTurkish());
 * </pre>
 * <p>
 * </p>
 *
 * @author Ahmet Alp Balkan <ahmet at ahmetalpbalkan.com>
 */
public class Deasciifier {

    static int turkishContextSize = 10;

    static HashMap<String, HashMap<String, Integer>> turkishPatternTable = null;

    static HashMap<String, String> turkishAsciifyTable = new HashMap<>();

    static {
        turkishAsciifyTable.put("ç", "c");
        turkishAsciifyTable.put("Ç", "C");
        turkishAsciifyTable.put("ğ", "g");
        turkishAsciifyTable.put("Ğ", "G");
        turkishAsciifyTable.put("ö", "o");
        turkishAsciifyTable.put("Ö", "O");
        turkishAsciifyTable.put("ı", "i");
        turkishAsciifyTable.put("İ", "I");
        turkishAsciifyTable.put("ş", "s");
        turkishAsciifyTable.put("Ş", "S");
    }

    static String[] uppercaseLetters = {"A", "B", "C", "D", "E", "F", "G",
            "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T",
            "U", "V", "W", "X", "Y", "Z"};

    static HashMap<String, String> turkishDowncaseAsciifyTable = new HashMap<>();

    static {
        for (String c : uppercaseLetters) {
            turkishDowncaseAsciifyTable.put(c, c.toLowerCase(Locale.US));
            turkishDowncaseAsciifyTable.put(c.toLowerCase(Locale.US), c.toLowerCase(Locale.US));
        }

        turkishDowncaseAsciifyTable.put("ç", "c");
        turkishDowncaseAsciifyTable.put("Ç", "c");
        turkishDowncaseAsciifyTable.put("ğ", "g");
        turkishDowncaseAsciifyTable.put("Ğ", "g");
        turkishDowncaseAsciifyTable.put("ö", "o");
        turkishDowncaseAsciifyTable.put("Ö", "o");
        turkishDowncaseAsciifyTable.put("ı", "i");
        turkishDowncaseAsciifyTable.put("İ", "i");
        turkishDowncaseAsciifyTable.put("ş", "s");
        turkishDowncaseAsciifyTable.put("Ş", "s");
        turkishDowncaseAsciifyTable.put("ü", "u");
        turkishDowncaseAsciifyTable.put("Ü", "u");
    }

    static HashMap<String, String> turkishUpcaseAccentsTable = new HashMap<>();

    static {
        for (String c : uppercaseLetters) {
            turkishUpcaseAccentsTable.put(c, c.toLowerCase(Locale.US));
            turkishUpcaseAccentsTable.put(c.toLowerCase(Locale.US), c.toLowerCase(Locale.US));
        }

        turkishUpcaseAccentsTable.put("ç", "C");
        turkishUpcaseAccentsTable.put("Ç", "C");
        turkishUpcaseAccentsTable.put("ğ", "G");
        turkishUpcaseAccentsTable.put("Ğ", "G");
        turkishUpcaseAccentsTable.put("ö", "O");
        turkishUpcaseAccentsTable.put("Ö", "O");
        turkishUpcaseAccentsTable.put("ı", "I");
        turkishUpcaseAccentsTable.put("İ", "i");
        turkishUpcaseAccentsTable.put("ş", "S");
        turkishUpcaseAccentsTable.put("Ş", "S");
        turkishUpcaseAccentsTable.put("ü", "U");
        turkishUpcaseAccentsTable.put("Ü", "U");
    }

    static HashMap<String, String> turkishToggleAccentTable = new HashMap<>();

    static {
        turkishToggleAccentTable.put("c", "ç"); // initial direction
        turkishToggleAccentTable.put("C", "Ç");
        turkishToggleAccentTable.put("g", "ğ");
        turkishToggleAccentTable.put("G", "Ğ");
        turkishToggleAccentTable.put("o", "ö");
        turkishToggleAccentTable.put("O", "Ö");
        turkishToggleAccentTable.put("u", "ü");
        turkishToggleAccentTable.put("U", "Ü");
        turkishToggleAccentTable.put("i", "ı");
        turkishToggleAccentTable.put("I", "İ");
        turkishToggleAccentTable.put("s", "ş");
        turkishToggleAccentTable.put("S", "Ş");
        turkishToggleAccentTable.put("ç", "c"); // other direction
        turkishToggleAccentTable.put("Ç", "C");
        turkishToggleAccentTable.put("ğ", "g");
        turkishToggleAccentTable.put("Ğ", "G");
        turkishToggleAccentTable.put("ö", "o");
        turkishToggleAccentTable.put("Ö", "O");
        turkishToggleAccentTable.put("ü", "u");
        turkishToggleAccentTable.put("Ü", "U");
        turkishToggleAccentTable.put("ı", "i");
        turkishToggleAccentTable.put("İ", "I");
        turkishToggleAccentTable.put("ş", "s");
        turkishToggleAccentTable.put("Ş", "S");
    }


    private String asciiString;
    private String turkishString;

    public Deasciifier() {
        loadPatternTable();
    }

    public void setAsciiString(String asciiString) {
        this.asciiString = asciiString;
        this.turkishString = asciiString;
    }

    public Deasciifier(String asciiString) {
        this();
        this.asciiString = asciiString;
        this.turkishString = asciiString;
    }

    public void printTurkishString() {
        System.out.println(turkishString); // with a trailing new line
    }

    public void printTurkishString(PrintWriter writer) {
        writer.println(turkishString); // without a trailing new line
    }

    public static String setCharAt(String mystr, int pos, String c) {
        return mystr.substring(0, pos).concat(c).concat(
                mystr.substring(pos + 1, mystr.length()));
    }

    public String turkishToggleAccent(String c) {
        String result = turkishToggleAccentTable.get(c);
        return (result == null) ? c : result;
    }

    public static String repeatString(String haystack, int times) {
        StringBuilder tmp = new StringBuilder();
        for (int i = 0; i < times; i++)
            tmp.append(haystack);

        return tmp.toString();
    }

    public boolean turkishMatchPattern(HashMap<String, Integer> dlist, int point) {
        int rank = dlist.size() * 2;
        String str = turkishGetContext(turkishContextSize, point);

        int start = 0;
        int end = 0;
        int _len = str.length();

        while (start <= turkishContextSize) {
            end = turkishContextSize + 1;
            while (end <= _len) {
                String s = str.substring(start, end);

                Integer r = dlist.get(s);

                if (r != null && Math.abs(r) < Math.abs(rank)) {
                    rank = r;
                }
                end++;
            }
            start++;
        }
        return rank > 0;
    }

    public boolean turkishMatchPattern(HashMap<String, Integer> dlist) {
        return turkishMatchPattern(dlist, 0);
    }

    public static String charAt(String source, int index) {
        return Character.toString(source.charAt(index));
    }

    public String turkishGetContext(int size, int point) {
        String s = repeatString(" ", (1 + (2 * size)));
        s = setCharAt(s, size, "X");

        int i = size + 1;
        boolean space = false;
        int index = point;
        index++;

        String currentChar;

        while (i < s.length() && !space && index < asciiString.length()) {
            currentChar = charAt(turkishString, index);

            String x = turkishDowncaseAsciifyTable.get(currentChar);

            if (x == null) {
                if (!space) {
                    i++;
                    space = true;
                }
            } else {
                s = setCharAt(s, i, x);
                i++;
                space = false;
            }
            index++;
        }

        s = s.substring(0, i);

        index = point;
        i = size - 1;
        space = false;

        index--;

        while (i >= 0 && index >= 0) {
            currentChar = charAt(turkishString, index);
            String x = turkishUpcaseAccentsTable.get(currentChar);

            if (x == null) {
                if (!space) {
                    i--;
                    space = true;
                }
            } else {
                s
                        = setCharAt(s, i, x);
                i--;
                space = false;
            }
            index--;
        }

        return s;
    }

    public boolean turkishNeedCorrection(String c, int point) {

        String tr = turkishAsciifyTable.get(c);
        if (tr == null)
            tr = c;

        HashMap<String, Integer> pl = turkishPatternTable.get(tr.toLowerCase(Locale.US));

        boolean m = false;
        if (pl != null) {
            m = turkishMatchPattern(pl, point);
        }

        if (tr.equals("I")) {
            if (c.equals(tr)) {
                return !m;
            } else {
                return m;
            }
        } else {
            if (c.equals(tr)) {
                return m;
            } else {
                return !m;
            }
        }
    }

    public boolean turkishNeedCorrection(String c) {
        return turkishNeedCorrection(c, 0);
    }

    /**
     * Convert a string with ASCII-only letters into one with Turkish letters.
     *
     * @return Deasciified text.
     */
    public String convertToTurkish() {
        for (int i = 0; i < turkishString.length(); i++) {
            String c = charAt(turkishString, i);

            if (turkishNeedCorrection(c, i)) {
                turkishString = setCharAt(turkishString, i,
                        turkishToggleAccent(c));
            } else {
                turkishString = setCharAt(turkishString, i, c);
            }
        }
        return turkishString;
    }

    public String turkishGetContext() {
        return turkishGetContext(turkishContextSize, 0);
    }

    public static String readFromFile(String filePath) {
        StringBuilder s = new StringBuilder();
        File f = new File(filePath);

        Scanner scan;
        try {
            scan = new Scanner(f);
            while (scan.hasNext()) {
                String line = scan.nextLine();
                if (line != null) {
                    s.append(line); // + "\n" ?
                }
            }
            scan.close();
        } catch (FileNotFoundException e) {
            System.out.println(e);
            e.printStackTrace();
        }
        return s.toString();
    }

    public static void savePatternTable(String filename) {

        try {
            FileOutputStream f = new FileOutputStream(filename);
            ObjectOutputStream out = new ObjectOutputStream(f);
            out.writeObject(turkishPatternTable);
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadPatternTable() {
        if (turkishPatternTable != null) return;

        turkishPatternTable = new HashMap<>();
        InputStream is = this.getClass().getResourceAsStream("/patterns/turkishPatternTable");

        try {
            ObjectInputStream ois = new ObjectInputStream(is);

            turkishPatternTable = (HashMap) ois.readObject();
            ois.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
