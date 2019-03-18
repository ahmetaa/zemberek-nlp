package zemberek.examples.normalization;

import java.io.IOException;
import java.util.List;
import zemberek.core.logging.Log;
import zemberek.morphology.TurkishMorphology;
import zemberek.normalization.TurkishSpellChecker;
import zemberek.tokenization.TurkishTokenizer;
import zemberek.tokenization.Token;
import zemberek.tokenization.Token.Type;


public class CorrectDocument {

  public static void main(String[] args) throws IOException {

    // This is only for demonstration. Current normalization mechanism is not proper for automatic text normalization.

    String input =
        "Türk Vatanı ve Milletinin ebedi varlığxını ve Yüce Türk Devlatinin bölünmez bütünlüğünü belirleyen bu Anayasa,\n"
            + "Türkiye Cumhuriyetinin kurucusu, ölümsüz önder ve eşsiz kahraman Atatürk’ün belirlediği milliyetçilik anlayışı ve onun\n"
            + "inkılap ve ilkeleri doğrultusunda;\n"
            + "Dünya milletleri ailesinin eşit haklara sahip şerefli bir üyesi olarak, Türkiye Cumhuriyetinin ebedi varlığı, refahı,\n"
            + "maddi ve manelvi mutluluğu ile çağdaş medeniyet düzeyine ulaşma azmi yönünde;\n"
            + "Millet iradesinin mutlak üstünlüğü, egemenliğin kayıtsız şartsız Türk Milletine ait olduğu ve bunu millet adına\n"
            + "kullanmaya yetkili kılınan hiçbir kişi ve kuruluşun, bu Anayasada gösterilen hürriyetçi demokrasi ve bunun icaplarıyla\n"
            + "belirlenmiş hukuk düzeni dışına çıkamayacağı;\n"
            + "Kuvvetler ayrımının, Devlet organları arasında üstünlük sıralaması anlamına gelmeyip, belli Devlet yetki ve\n"
            + "görevlerinin kullanılmasından ibaret ve bununla sınırlı medeni bir işbölümü ve işbirliği olduğu ve üstünlüğün ancak Anayasa\n"
            + "ve kanunlarda bulunduğu;\n"
            + "Hiçbir faaliyetin Türk milli menfaatlerinin, Türk varlığının, Devleti ve ülkesiyle bölünmezliği esasının, Türklüğün\n"
            + "tarihi ve manevi değerlerinin, Atatürk milliyetçiliği, ilke ve inkılapları ve medeniyetçiliğinin karşısında korunma\n"
            + "göremeyeceği ve laiklik ilkesinin gereği olarak kutsal din duygularının, Devlet işlerine ve politikaya kesinlikle\n"
            + "karıştırılamayacağı; (5)\n"
            + "\n"
            + "Her Türk vatandaşının bu Anayasadaki temel hak ve hürriyetlerden eşitlik ve sosyal adalet gereklerince yararlanarak\n"
            + "milli kültür, medeniyet ve hukuk düzeni içinde onurlu bir hayat sürdürme ve maddi ve manevi varlığını bu yönde geliştirme\n"
            + "hak ve yetkisine doğuştan sahip olduğu;\n"
            + "–––––––––––––––––––––––––––––\n"
            + "(1) Bu Anayasa; Kuruczu Meclis tarafından 18/10/1982’de Halkoylamasına sunulmak üzere kabul edilmiş ve 20/10/1982\n"
            + "tarihli ve 17844 sayılı Resmî Gazete’de yayımlanmış; 7/11/1982’de Halkoylamasına sunulduktan sonra 9/11/1982 tarihli\n"
            + "ve 17863 Mükerrer sayıli Resmî Gazete’de yeniden yayımlanmıştır.\n"
            + "(2) 7/5/2010 tarihli ve 5982 sayılı Kanun ile yapılan Anayasa değişiklikleri 12/9/2010 tarihinde Halkoyuna sunularak kabul\n"
            + "edilmiş, buna ilişkin 22/9/2010 tarihli ve 846 sayılı Yüksek Seçim Kurulu Kararı 23/9/2010 tarihli ve 27708 sayılı Resmî\n"
            + "Gazete’de yayımlanmıştır.\n"
            + "(3) 21/1/2017 tarihli ve 6771 sayılı Kanun ile yapılan Anayasa değişiklikleri 16/4/2017 tarihinde Halkoyuna sunularak kabul\n"
            + "edilmiş, buna ilişkin 27/4/2017 tarihli ve 663 sayılı Yüksek Seçim Kurulu Kararı 27/4/2017 tarihli ve 30050 Mükerrer\n"
            + "sayılı Resmî Gazete’de yayımlanmıştır.\n"
            + "(4) Anayasa’nın Başlangıc metni 23/7/1995 tarih ve 4121 sayılı Kanun’un 1 inci maddesi ile değiştirilmiş ve metne\n"
            + "işlenmiştir.\n"
            + "(5) Bu fıkrada geçen, “Hiçbir düşünce ve mülahazanın” ibaresi, 3/10/2001 tarih ve 4709 sayılı Kanunun 1 inci maddesiyle\n"
            + "“Hiçbir faaliyetin” şeklinde değiştirilmiş ve metne işlenmiştir.\n"
            + "130\n"
            + "Topluca Türk vatandaşlarının milli gurur ve iftiharlarda, milli sevinç ve kederlerde, milli varlığa karşı hak ve\n"
            + "ödevlerde, nimet ve külfetlerde ve millet hayatının her türlü tecellisinde ortak olduğu, birbirinin hak ve hürriyetlerine kesin\n"
            + "saygı, karşılıklı içten sevgi ve kardeşlik duygularıyla ve \"Yurtta sulh, cihanda sulh\" arzu ve inancı içinde, huzurlu bir hayat\n"
            + "talebine hakları bulunduğu;\n"
            + "FİKİR, İNANÇ VE KARARIYLA anlaşılmak, sözüne ve ruhuna bu yönde saygı ve mutlak sadakatle yorumlanıp\n"
            + "uygulanmak üzere.\n"
            + "TÜRK MİLLETİ TARAFINDAN, demokrasiye aşık Türk evlatlarının vatan ve millet sevgisine emanet ve tevdi\n"
            + "olunur.\n"
            + "BİRİNCİ KISIM\n"
            + "GENEL ESASLAR\n"
            + "I. Devletin şekli\n"
            + "Madde 1 – Türkiye Devleti bir Cumhuriyettir.\n"
            + "II. Cumhuriyetin nitelikleri\n"
            + "Madde 2 – Türkiye Cumhuriyeti, toplumun huzuru, milli dayanışma ve adalet anlayışı içinde, insan haklarına saygılı,\n"
            + "Atatürk milliyetçiliğine bağlı, başlangıçta belirtilen temel ilkelere dayanan, demokratik, laik ve sosyal bir hukuk Devletidir.\n"
            + "III. Devletin bütünlüğü, Resmî dili, bayrağı, milli marşı ve başkenti\n"
            + "Madde 3 – Türkiye Devleti, ülkesi ve milletiyle bölünmez bir bütündür. Dili Türkçedir.\n"
            + "Bayrağı, şekli kanununda belirtilen, beyaz ay yıldızlı al bayraktır.\n"
            + "Milli marşı \"İstiklal Marşı\"dır.\n"
            + "Başkenti Ankara'dır.\n"
            + "IV. Değiştirilemeyecek hükümler\n"
            + "Madde 4 – Anayasanın 1 inci maddesindeki Devletin şeklinin Cumhuriyet olduğu hakkındaki hüküm ile, 2 nci\n"
            + "maddesindeki Cumhuriyetin nitelikleri ve 3 üncü maddesi hükümleri değiştirilemez ve değiştirilmesi teklif edilemez.\n"
            + "V. Devletin temel amaç ve görevleri\n"
            + "Madde 5 – Devletin temel amaç ve görevleri, Türk milletinin bağımsızlığını ve bütünlüğünü, ülkenin bölünmezliğini,\n"
            + "Cumhuriyeti ve demokrasiyi korumak, kişilerin ve toplumun refah, huzur ve mutluluğunu sağlamak; kişinin temel hak ve\n"
            + "hürriyetlerini, sosyal hukuk devleti ve adalet ilkeleriyle bağdaşmayacak surette sınırlayan siyasal, ekonomik ve sosyal\n"
            + "engelleri kaldırmaya, insanın maddi ve manevi varlığının gelişmesi için gerekli şartları hazırlamaya çalışmaktır.\n"
            + "VI. Egemenlik\n"
            + "Madde 6 – Egemenlik, kayıtsız şartsız Milletindir.\n"
            + "Türk Milleti, egemenliğini, Anayasanın koyduğu esaslara göre, yetkili organları eliyle kullanır.\n"
            + "Egemenliğin kullanılması, hiçbir surette hiçbir kişiye, zümreye veya sınıfa bırakılamaz. Hiçbir kimse veya organ\n"
            + "kaynağını Anayasadan almayan bir Devlet yetkisi kullanamaz";

    TurkishTokenizer tokenizer = TurkishTokenizer.ALL;
    TurkishMorphology morphology = TurkishMorphology.createWithDefaults();
    TurkishSpellChecker spellChecker = new TurkishSpellChecker(morphology);
    StringBuilder output = new StringBuilder();

    for (Token token : tokenizer.tokenize(input)) {
      String text = token.getText();
      if (analyzeToken(token) && !spellChecker.check(text)) {
        List<String> strings = spellChecker.suggestForWord(token.getText());
        if (!strings.isEmpty()) {
          String suggestion = strings.get(0);
          Log.info("Correction: " + text + " -> " + suggestion);
          output.append(suggestion);
        } else {
          output.append(text);
        }
      } else {
        output.append(text);
      }
    }
    Log.info(output);
  }

  static boolean analyzeToken(Token token) {
    return token.getType() != Type.NewLine
        && token.getType() != Type.SpaceTab
        && token.getType() != Type.UnknownWord
        && token.getType() != Type.RomanNumeral
        && token.getType() != Type.Unknown;
  }
}
