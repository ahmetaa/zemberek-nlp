package zemberek.corpus;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import zemberek.core.text.Regexps;
import zemberek.core.text.TextUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.regex.Pattern;

public class WebDocument {
    String source;
    String id;
    String url;
    String crawlDate;
    String labelString;
    String category;
    String title;
    long hash;

    List<String> lines = new ArrayList<>();

    public WebDocument(String source,
                       String id,
                       String title,
                       List<String> lines,
                       String url,
                       String crawlDate,
                       String labels,
                       String category) {
        this.source = source;
        this.id = id;
        this.lines = lines;
        this.url = url;
        this.crawlDate = crawlDate;
        this.labelString = labels;
        this.category = category;
        this.title = title;
        this.hash = contentHash();
    }

    public int contentLength() {
        int total = 0;
        for (String line : lines) {
            total += line.length();
        }
        return total;
    }

    public void removeDuplicateLines() {
        lines = new ArrayList<>(new LinkedHashSet<>(lines));
        contentHash();
    }

    public String getDocumentHeader() {
        return "<doc id=\"" + id + "\" source=\"" + source + "\" title=\"" + title + "\" crawl-date=\"" + crawlDate +
                "\" labels=\"" + labelString + "\" category=\"" + category + "\">";
    }

    public WebDocument emptyContent() {
        return new WebDocument(
                this.source,
                this.id,
                "",
                Collections.emptyList(),
                this.url,
                "", "", "");
    }

    public String getLabelString() {
        return labelString;
    }

    public String getCategory() {
        return category;
    }

    public List<String> getLabels() {
        return Splitter.on(",").omitEmptyStrings().trimResults().splitToList(labelString);
    }

    public String getTitle() {
        return title;
    }

    static Pattern sourcePattern = Pattern.compile("(source=\")(.+?)(\")");
    static Pattern urlPattern = Pattern.compile("(id=\")(.+?)(\")");
    static Pattern crawlDatePattern = Pattern.compile("(crawl-date=\")(.+?)(\")");
    static Pattern labelPattern = Pattern.compile("(labels=)(.+?)(\")");
    static Pattern categoryPattern = Pattern.compile("(category=)(.+?)(\")");
    static Pattern titlePattern = Pattern.compile("(title=)(.+?)(\")");

    public static WebDocument fromText(String meta, List<String> pageData) {

        String url = Regexps.firstMatch(urlPattern, meta, 2);
        String id = url.replaceAll("http://|https://", "");
        String source = Regexps.firstMatch(sourcePattern, meta, 2);
        String crawlDate = Regexps.firstMatch(crawlDatePattern, meta, 2);
        String labels = getAttribute(labelPattern, meta);
        String category = getAttribute(categoryPattern, meta);
        String title = getAttribute(titlePattern, meta);

        int i = source.lastIndexOf("/");
        if (i >= 0 && i < source.length()) {
            source = source.substring(i + 1);
        }
        return new WebDocument(source, id, title, pageData, url, crawlDate, labels, category);
    }

    private static String getAttribute(Pattern pattern, String content) {
        String str = Regexps.firstMatch(pattern, content, 2);
        str = str == null ? "" : str.replace('\"', ' ').trim();
        return TextUtil.convertAmpresandStrings(str);
    }

    private long contentHash() {
        String contentNoSpaces = String.join(" ", lines).replaceAll("\\s+", "");
        return com.google.common.hash.Hashing.murmur3_128().hashUnencodedChars(contentNoSpaces).asLong();
    }

    public long getHash() {
        return hash;
    }

    public String getContentAsString() {
        return Joiner.on("\n").join(lines);
    }

    public List<String> getLines() {
        return lines;
    }

    public void setContent(List<String> lines) {
        this.lines = lines;
        this.hash = contentHash();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        WebDocument page = (WebDocument) o;

        return page.contentHash() == this.contentHash();
    }

    public String getSource() {
        return source;
    }

    public String getId() {
        return id;
    }

    public String getUrl() {
        return url;
    }

    @Override
    public int hashCode() {
        long h = contentHash();
        return (int) ((h & 0xffffffffL) ^ (h >> 32));
    }

}
