package zemberek.langid;

import junit.framework.Assert;
import org.junit.Test;

import java.io.IOException;

public class LanguageIndentifierTest {
    @Test
    public void modelGroupTest() throws IOException {
        LanguageIdentifier lid = LanguageIdentifier.fromInternalModels();
        Assert.assertEquals("tr", lid.identify("merhaba dünya ve tüm gezegenler"));
        Assert.assertEquals("en", lid.identify("hello world and all the planets what is this?"));
        Assert.assertEquals("fr", lid.identify("Bonjour tout le monde et toutes les planètes"));
        Assert.assertEquals("az", lid.identify("Salam dünya və bütün planetlərin bu həqiqətən pis olur"));
    }

    @Test
    public void allModelTest() throws IOException {
        LanguageIdentifier lid = LanguageIdentifier.fromInternalModels();
        Assert.assertEquals("tr", lid.identify("merhaba dünya ve tüm gezegenler"));
        Assert.assertEquals("es", lid.identify("Hola mundo y todos los planetas"));
    }

    @Test
    public void testContainsLanguage() throws IOException {
        LanguageIdentifier lid = LanguageIdentifier.fromInternalModels();
        String tr_es = "merhaba dünya ve tüm gezegenler Hola mundo y todos los planetas";
        Assert.assertTrue(lid.containsLanguage(tr_es, "tr", 20));
        Assert.assertTrue(lid.containsLanguage(tr_es, "es", 20));

        Assert.assertFalse(lid.containsLanguage(tr_es, "ar", 20));

        String es_en = "Hola mundo y todos los planetas " +
                "The state is that great fiction by which everyone tries to live at the expense of everyone else";
        Assert.assertTrue(lid.containsLanguage(es_en, "es", 20));
        Assert.assertTrue(lid.containsLanguage(es_en, "en", 20));
        Assert.assertFalse(lid.containsLanguage(es_en, "tr", 20));
        Assert.assertFalse(lid.containsLanguage(es_en, "ar", 20));
    }

    @Test
    public void getLanguagesTest() throws IOException {
        LanguageIdentifier lid = LanguageIdentifier.fromInternalModelGroup("tr_group");
        Assert.assertTrue(lid.getLanguages().contains("tr"));
        Assert.assertTrue(lid.getLanguages().contains("en"));
        Assert.assertFalse(lid.getLanguages().contains("unk"));
        Assert.assertFalse(lid.getLanguages().contains("ar"));
    }

}
