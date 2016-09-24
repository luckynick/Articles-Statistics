import javafx.util.Pair;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Attributes;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ArticleBuilder
{
    private Collector collector;
    private long articlesExeTime = 0;
    private Date runTime; //start of massBuild

    ArticleBuilder(Collector col)
    {
        collector = col;
        runTime = new Date();
    }

    public Article build(String targetLink)
    {
        //test
        System.out.println(targetLink);

        long start = System.currentTimeMillis();
        Document doc = null;
        Article a = null;

        try
        {
            a = new Article(targetLink);
        }
        catch(MalformedURLException ex)
        {
            collector.writeToLog(ex.getMessage());
            return null;
        }
        if(targetLink.contains("bzh.life")) a.setIndicator(Indicator.SUSPENDED); //bzh is slow and programs works wrong with it
        try
        {
            if(a.getIndicator() == Indicator.SUCCESS)
            {
                Connection c = Jsoup.connect(targetLink).userAgent("Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko)" +
                        " Chrome/49.0.2623.87 Safari/537.36");
                c.timeout(4000);
                doc = c.get();
            }
        }
        catch(SocketTimeoutException e)
        {
            collector.writeToLog("Socket time out");
            a.setIndicator(Indicator.TIMEOUT);
        }
        catch (IOException e)
        {
            collector.writeToLog(e.getMessage());
            a.setIndicator(Indicator.IOEXCEPTION);
        }
        if(doc == null) return a;
        if(a.getIndicator() == Indicator.SUCCESS)
        {
            doc.charset(Charset.forName("UTF-8"));
            acumulateData(doc, a);
        }
        a.setLoadTime(System.currentTimeMillis() - start);
        a.lock();

        //test
        System.out.println(a.getTitle());
        System.out.println(a.getType());
        System.out.println(a.getIndicator().toString());
        System.out.println(a.getAuthor());
        System.out.println(a.getDatePublished());
        System.out.println(a.getSeen() + " seen, " + a.getCommented() + " commented");
        System.out.println(a.getLoadTime() + " milliseconds");
        System.out.println();

        return a;
    }

    public List<Article> massBuild(String host) throws IOException
    {
        long start = System.currentTimeMillis();
        Article a;

        List<Article> result = new ArrayList<>();
        List<URL> failed = new ArrayList<>();
        List<String> linksWithDuplicates = new ArrayList<>();

        runTime = GregorianCalendar.getInstance().getTime();
        int success = 0, all = 0;
        Connection c = Jsoup.connect(host).userAgent("Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko)" +
                " Chrome/49.0.2623.87 Safari/537.36");
        c.timeout(1000);
        Document d = c.get();
        Elements els = d.select("div[class^=article] a[href], " + // pravda
                "tr a[href], div[class^=post] a[href]"); // tabloid
        for(Element e : els)
        {
            String link;
            String href = e.attr("href");
            if (href.contains(host)) link = href;
            else
            {
                if (href.contains("http://")) link = href;
                else link = host + href.substring(1, href.length());
            }
            if(!linksWithDuplicates.contains(link)) linksWithDuplicates.add(link); //eliminate duplicates
        }
        for (String link : linksWithDuplicates)
        {
            all++;
            a = build(link);
            if(a.getIndicator() == Indicator.SUCCESS) success++;
            else failed.add(new URL(link));
            result.add(a);
            articlesExeTime += a.getLoadTime();
        }
        articlesExeTime = System.currentTimeMillis() - start;

        //test
        System.out.println("\n" + success + " of " + all + " succeded.\n\n");
        System.out.println("\nFailed articles:");
        for(URL url : failed)
        {
            System.out.println(url.toString());
        }

        return result;
    }

    private void setOG(Elements inHead, Article article)
    {
        for(Element e: inHead)
        {
            Attributes atts = e.attributes();
            String value = atts.get("property");
            if(value.equals("og:title")) article.setTitle(atts.get("content"));
            if(value.equals("og:type")) article.setType(atts.get("content"));
            if(value.equals("og:description")) article.setDescription(atts.get("content"));
        }
    }

    private void acumulateData(Document doc, Article a)
    {
        Holder<String> outerContent = new Holder<>("");
        Elements inHead = doc.head().getElementsByTag("meta");
        setOG(inHead, a);
        Element body = doc.body();

        Element keyData = getKeyData(body, a.getSubSite());
        if(keyData == null)
        {
            a.setIndicator(Indicator.SELECTOR_ERROR);
            collector.writeToLog("keyData is null, no elements found by query");
            return;
        }
        a.setDatePublished(selectDate(keyData, outerContent, a));
        a.setAuthor(separateAuthor(outerContent, a));
        a.setSeen(separateSeen(outerContent.toString()));
        a.setCommented(separateCommented(outerContent.toString()));
        if(a.getSeen() == -1) a.setSeen(selectSeen(keyData));
        if(a.getCommented() == -1) a.setCommented(selectCommented(keyData));
        a.setSocial(selectSocial(keyData, a));
    }

    private Element getKeyData(Element body, String subSite)
    {
        Element keyData = body.select("div[align=center], " + //tabloid
                "div.layout, " + // pravda
                "div.block0, " + // epravda eurointegration blogs
                "div.ww1.white, " + // kiev
                "div.content, " + // champion
                "div.page"). // istpravda life
                select("div.w1, " + // tabloid
                "div.main-content, " + // pravda
                "div.pad0, " + // epravda eurointegration blogs istpravda life
                "div.bg-r, " + // kiev
                "div.ww1.white"). // champion
                select("div.w2, " + // tabloid
                "div.layout-main, " + // pravda
                "div.bg-l, " + // kiev
                "div.fblock, " + // epravda eurointegration blogs life
                "div.c1, " + // istpravda
                "div[align=left]"). // champion
                select("div div[style~=^padding:[a-z0-9A-Z\\s]+;$] > div.tt > div[style~=^clear:[a-zA-Z\\s]*;$] > div.tt2, " + // tabloid
                "div.cols.clearfix, " + // pravda
                //"div.cols.clearfix div.post.post_news" + // pravda
                "div.fblock > div.rpad, " + //  epravda eurointegration
                "div.block0 > div.rpad, " + // epravda eurointegration
                "div:not([id]).block-m0 > div.block0 div.padr, " + // kiev ( > div.bg3 > div.block4 >)
                "div.block61 > div.rpad, " + // eurointegration
                "div.bpost, " + // blogs !!!
                "div.article, " + // istpravda life
                "div.tt4 > div.tt6, " + // champion
                "div.tt41 > div.tit3").first(); // champion
        if (subSite.equals("blogs.pravda.com.ua")) keyData = keyData.parent();
        if (subSite.equals("bzh.life")) keyData = body.select("div.layout").select("div.content.instruction").first();
        return keyData;
    }

    private Date selectDate(Element keyData, Holder<String> outerContent, Article a)
    {
        Element dateElement = keyData.select("div.post_news__date, " + //pravda
                "div.tit3, " +
                "table div.source, " + // tabloid
                "div.date1, " +
                "span.dt2, " + //eurointegration
                "div.bpost > span.bdate, " +
                "div.dt1, " +
                "div.section-responsive div.article-date > span + span")
                .first();
        Locale locUA = Locale.forLanguageTag("uk-UA");
        Locale locRU = Locale.forLanguageTag("ru-RU");
        String datePattern = null;
        String timeString = null;
        String text = dateElement.text();
        text = text.replaceAll("(?:[\\u00a0\\u0097])+", " ").replaceAll("\\s*_\\s*", ", ").
                replaceAll("[Вв]ерсія для друку\\s?", "")
                .replaceAll("(([Пп]онеділок)|([Вв]івторок)|([Сс]ереда)|([Чч]етвер)|([Пп]\'ятниця)|([Сс]убота)|([Нн]еділя)|" +
                        "([Пп]онедельник)|([Вв]торник)|([Сс]реда)|([Чч]етверг)|" +
                        "([Пп]ятница)|([Сс]уббота)|([Вв]оскресенье)),?\\s?", "");
        Matcher universalMatcher = Pattern.compile("(?:([\\p{Punct}0-9\\p{L}\\s’№\\p{Pd}]*)(?:,?\\s))?" +
                "((?:[0-9]{2}\\.[0-9]{2}\\.[0-9]{4}(?:,?\\s[0-9]{2}:[0-9]{2})?)|" +
                "(?:(?:\\p{Lu}[\\p{Ll}']{5,8},\\s)?[0-9]{1,2}\\s\\p{L}{5,11},?\\s[0-9]{4}(?:,\\s[0-9]{2}:[0-9]{2})?))" +
                "(?:(?:,?\\s?)([\\p{Punct}0-9\\p{L}\\s’№\\p{Pd}]*))?").matcher(text);
        if(universalMatcher.find())
        {
            for(int i = 1; i < 4; i++)
            {
                //System.out.println("group " + i + ": " + universalMatcher.group(i));
                if(i == 2) timeString = universalMatcher.group(i);
                else
                {
                    outerContent.append(universalMatcher.group(i));
                    if(i != 3) outerContent.append("|");
                }
            }
        }
        if(timeString == null)
        {
            collector.writeToLog("timeString is null\nWhole text: " + text);
            a.setIndicator(Indicator.REGEX_PATTERN_ERROR);
            return null;
        }
//        timeString = timeString.replaceAll(",\\s", " ").replaceFirst("\\p{Lu}[\\p{Ll}']{5,8},\\s", "").replaceAll(",", "");
        DateFormat df;
        if(timeString.matches("(?:\\p{Lu}[\\p{Ll}']{5,8},\\s)?[0-9]{1,2}\\s\\p{L}{5,11},?\\s[0-9]{4}"))
        {
            timeString = timeString.replaceFirst("\\p{Lu}[\\p{Ll}']{5,8},\\s", "");
            datePattern = "d MMMM yyyy";
        }
        else if(timeString.matches("(?:(?:\\p{Lu}[\\p{Ll}']{5,8},\\s)?[0-9]{1,2}\\s\\p{L}{5,11},?\\s[0-9]{4},\\s[0-9]{2}:[0-9]{2})"))
        {
            timeString = timeString.replaceFirst("\\p{Lu}[\\p{Ll}']{5,8},\\s", "");
            datePattern = "d MMMM yyyy, HH:mm";
        }
        else if(timeString.matches("[0-9]{2}\\.[0-9]{2}\\.[0-9]{4}"))
            datePattern = "dd.MM.yyyy";
        else if(timeString.matches("([0-9\\.]{7,10},?\\s[0-9]{2}:[0-9]{2})"))
        {
            timeString = timeString.replaceAll(",", "");
            datePattern = "dd.MM.yyyy HH:mm";
        }

        if(datePattern == null)
        {
            collector.writeToLog("Date can't be parsed. timeString:\n" + timeString);
            a.setIndicator(Indicator.REGEX_PATTERN_ERROR);
            return null;
        }
        /*datePattern += " z";
        timeString += " EEST";*/ //check if program sets time wrong abroad
        df = new SimpleDateFormat(datePattern, locUA);
        Date d = null;
        try
        {
            d = df.parse(timeString);
        }
        catch (ParseException e)
        {
            try
            {
                d = new SimpleDateFormat(datePattern, locRU).parse(timeString); //try russian
            }
            catch (ParseException e1)
            {
                a.setIndicator(Indicator.PARSE_ERROR);
                collector.writeToLog("Neither ukrainian nor russian date format succeeded.");
            }
        }
        return d;
    }

    private String separateAuthor(Holder<String> containsAuthor, Article a)
    {
        Matcher dividerMat = Pattern.compile("(\\p{all}*)\\|(\\p{all}*)").matcher(containsAuthor.toString());
        Pattern nameSurnPat = Pattern.compile("(?:\\p{all}*?,?\\s?)" +
                "((?:\\p{Lu}\\.)?\\p{Lu}[\\p{Ll}’']+(?:\\p{Pd}\\p{Lu}[\\p{Ll}’']+)?\\s" +
                "(?:\\p{Lu}\\.)?\\p{Lu}[\\p{Ll}’']+(?:\\p{Pd}\\p{Lu}[\\p{Ll}’']+)?" +
                "(?:,\\s(?:\\p{Lu}\\.)?\\p{Lu}[\\p{Ll}’']+(?:\\p{Pd}\\p{Lu}[\\p{Ll}’']+)?\\s" +
                "(?:\\p{Lu}\\.)?\\p{Lu}[\\p{Ll}’']+(?:\\p{Pd}\\p{Lu}[\\p{Ll}’']+)?)*)" +
                "(?:,?\\s?\\p{all}*?)");
        String parts[] = new String[2];
        String nameSurn = null;
        if(dividerMat.find())
        {
            parts[0] = dividerMat.group(1);
            parts[1] = dividerMat.group(2);
        }
        else
        {
            collector.writeToLog("Can't divide containsAuthor. ContainsAuthor: \n" + containsAuthor);
            a.setIndicator(Indicator.REGEX_PATTERN_ERROR);
            return null;
        }
        for(int i = 0; i < parts.length; i++)
        {
            String s = parts[i];
            Matcher mat = nameSurnPat.matcher(s);
            if(mat.find())
            {
                nameSurn = mat.group(1);
                if(i == 0) containsAuthor.set(parts[1]);
                else containsAuthor.set(parts[0]);
            }
        } //No pattern error possible. No match found means there is no author in string or format of author is strange
        return nameSurn;
    }

    public int separateSeen(String content)
    {
        Matcher numViewsMat = Pattern.compile("\\p{all}*?([0-9]+)\\s*[Пп]ерегляд\\p{Ll}{1,5}\\p{all}*?").matcher(content);
        if(numViewsMat.find())
        {
            return Integer.parseInt(numViewsMat.group(1));
        }
        return -1;
    }

    public int separateCommented(String content)
    {
        Matcher numCommentsMat = Pattern.compile("\\p{all}*?[Кк]омент[арі]{0,3}\\s*\\[?\\s*([0-9]+)\\s*\\]?\\p{all}*?").matcher(content);
        if(numCommentsMat.find())
        {
            return Integer.parseInt(numCommentsMat.group(1));
        }
        return -1;
    }

    public int selectSeen(Element keyData)
    {
        Element seenElement = keyData.select("div.post__views").first();
        if(seenElement == null) return -1;
        Matcher m = Pattern.compile("\\p{all}*?([0-9]*+)\\p{all}*?").matcher(seenElement.text());
        if(m.find())
        {
            return Integer.parseInt(m.group(1));
        }
        return -1;
    }

    public int selectCommented(Element keyData)
    {
        Element commentedElement = keyData.select("div.post__comments, a.but5, span.fb_comments_count, " +
                "div.tit4").first(); //javascript changes html code after document is loaded!!!
        if(commentedElement == null) return -1;
        Matcher m = Pattern.compile("\\p{all}*?([0-9]*+)\\p{all}*?").matcher(commentedElement.text());
        if(m.find())
        {
            return Integer.parseInt(m.group(1));
        }
        return -1;
    }

    public List<Pair<String, Integer>> selectSocial(Element keyData, Article a)
    {
        Element socialBar = keyData.select("div.post__social").first();

        return null;
    }

    /**
     * Checks if indicators of Article are negative
     * @return true if article has errors, false if SUCCESS or SUSPENDED
     */
    public boolean error(Indicator indicator)
    {
        if(indicator == Indicator.SUCCESS || indicator == Indicator.SUSPENDED || indicator == Indicator.EARLY_DATA) return false;
        else return true;
    }

    public<K, V> void printPairs(List<Pair<K, V>> list)
    {
        for(Pair<K, V> p : list)
        {
            System.out.println(p.getKey().toString() + "  " + p.getValue().toString());
        }
    }

    public Collector getCollector()
    {
        return collector;
    }

    public long getArticlesExeTime()
    {
        return articlesExeTime;
    }
}

class Holder<T>
{
    private T object;

    public Holder(T ob)
    {
        object = ob;
    }

    public void set(T ob)
    {
        object = ob;
    }

    public T get()
    {
        return object;
    }

    @Override
    public String toString()
    {
        return object.toString();
    }

    public void append(String ob)
    {
        if(object instanceof String)
        {
            object = (T)new String(object + ob);
        }
    }
}