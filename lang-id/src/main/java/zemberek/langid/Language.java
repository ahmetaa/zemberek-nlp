package zemberek.langid;

import com.google.common.collect.Sets;

import java.util.Arrays;
import java.util.Locale;
import java.util.Set;

public enum Language {
    AR("Arabic"), AZ("Azeri"),
    BA("Bashkir"), BE("Belarusian"), BG("Bulgarian"), BS("Bosnian"),
    CA("Catalan"), CE("Chechen"), CK("Kurdish-Sorani"), CS("Czech"), CV("Chuvash"),
    DA("Danish"), DE("German"),
    EL("Greek"), EN("English"), EO("Esperanto"), ES("Spanish"), ET("Estonian"), EU("Basque"),
    FA("Persian"), FI("Finnish"), FR("French"),
    HE("Hebrew"), HI("Hindi"), HR("Croatian"), HU("Hungarian"), HY("Armenian"),
    ID("Indonesian"), IS("Icelandic"), IT("Italian"),
    JA("Japanese"), JV("Javanese"),
    KA("Georgian"), KK("Kazakh"), KM("Khmer"), KO("Korean"), KU("Kurdish"), KY("Krgyz"),
    LA("Latin"), LT("Lithuanian"), LV("Latvian"),
    ML("Malayalam"), MN("Mongolian"), MS("Malay"), MY("Burmese"),
    NL("Dutch"), NO("Norwegian"),
    PL("Polish"), PT("Portuguese"),
    RO("Romanian"), RU("Russian"),
    SK("Slovak"), SL("Slovene"), SR("Serbian"), SV("Swedish"),
    TR("Turkish"),
    UK("Ukranian"), UZ("Uzbek"),
    VI("Vietnamese"),
    WAR("Waray"),
    ZH("Chinese");

    public String id;
    public String name;

    Language(String name) {
        this.id = name().toLowerCase(Locale.ENGLISH);
        this.name = name;
    }

    public static Language getByName(String input) {
        for (Language language : Language.values()) {
            if (language.id.equalsIgnoreCase(input))
                return language;
        }
        throw new IllegalArgumentException("Cannot find language with name:" + input);
    }

    public static String[] allLanguages() {
        String[] ids = new String[Language.values().length];
        int i = 0;
        for (Language l : Language.values()) {
            ids[i++] = l.id;
        }
        return ids;
    }

    public static Set<String> languageIdSet() {
        return Sets.newLinkedHashSet(Arrays.asList(allLanguages()));
    }
}