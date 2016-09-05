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
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class Article
{
    public static ArrayList<URL> urlArrayList = new ArrayList<>();

    public Collector col; //for logging

    private Document doc;
    private long start;

    private String title;
    private Date datePublished;
    private URL url;
    private String type;
    private String subSite;
    private String author;
    private String[] topics;
    private int seen/* = -1*/, commented/* = -1*/;
    private long loadTime = 0; //in millisec
    private Map<String, Integer> social;
    private Indicator indicator = Indicator.SUCCESSFUL; //0 successful, -1 timeout, -2 selector query error, -3 parse error, 1 early data, 2

    @Deprecated
    public static void main(String args[]) throws Exception
    {
        if(args.length != 2) args = new String[]{"", ""};
        String testURL = "http://www.epravda.com.ua/cdn/cd1/2016/09/uroki-devalvacii/index.html"; //
        Collector testCollector = new Collector(null, null)
        {
            @Override
            public void writeToLog(String what)
            {
                System.out.println("\u001B[31m" + what + "\u001B[0m");
            }
        };
        int duplicates = 0;
        long start = System.currentTimeMillis();
        if(true)
        {
            ArrayList<URL> al = new ArrayList<>();
            int success = 0, all = 0;
            String target = "http://www.pravda.com.ua/";
            Connection c = Jsoup.connect(target).userAgent("Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko)" +
                    " Chrome/49.0.2623.87 Safari/537.36");
            c.timeout(5000);
            Document d = c.get();
            Elements els = d.select("div[class^=article]" + args[0] + " a[href]" + args[1] + ", " + // pravda
                    "tr a[href], div[class^=post] a[href]"); // tabloid
            for (Element e : els)
            {
                all++;
                Article a = null;
                String link = null;
                try
                {
                    String href = e.attr("href");
                    if (href.contains(target)) link = href;
                    else
                    {
                        if (href.contains("http://")) link = href;
                        else link = target + href.substring(1, href.length());
                    }
                    if(link.contains("bzh.life")) continue;
                    a = new Article(link, testCollector);
                    if(a.datePublished != null) success++;
                    if(urlArrayList.contains(a.url)) duplicates++;
                    urlArrayList.add(a.url);
                }
                catch (NullPointerException exc)
                {
                    testCollector.writeToLog("Null pointer ex");
                    al.add(new URL(link));
                    continue;
                }
//            if(a != null && !a.author.equals("null")) System.out.println(a.author);
                //if(a.datePublished == null) al.add(a);
            }
            System.out.println("\n" + success + " of " + all + " succeded.\n\n");
            System.out.println(duplicates + " duplicates");
            System.out.println("\nFailed articles:");
            for(URL url : al)
            {
                System.out.println(url.toString());
            }
        }
        else
        {
            Article a = new Article(testURL, testCollector);
        }
        System.out.println(System.currentTimeMillis() - start + " milliseconds");
    }

    public Article(String url, Collector ob)
    {
        long start = System.currentTimeMillis();
        col = ob;
        try
        {
            this.url = new URL(url);
        }
        catch (MalformedURLException e)
        {
            col.writeToLog(e.getMessage());
            return;
        }
        try
        {
            Connection c = Jsoup.connect(url).userAgent("Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko)" +
                    " Chrome/49.0.2623.87 Safari/537.36");
            c.timeout(5000);
            doc = c.get();
        }
        catch(SocketTimeoutException e)
        {
            col.writeToLog("Socket time out");
            indicator = Indicator.TIMEOUT;
            return;
        }
        catch (IOException e)
        {
            col.writeToLog(e.getMessage());
            return;
        }
        doc.charset(Charset.forName("UTF-8"));
        acumulateData();

        loadTime = System.currentTimeMillis() - start;
    }

    private void setTitleAndType(Elements inHead)
    {
        for(Element e: inHead)
        {
            Attributes atts = e.attributes();
            String value = atts.get("property");
            if(value.equals("og:title")) title = atts.get("content");
            if(value.equals("og:type")) type = atts.get("content");
        }
    }

    private Element getKeyData(Element body)
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
                "div:not([id]).block-m0 > div.block0 > div.bg3 > div.block4 > div.padr, " + // kiev
                "div.block61 > div.rpad, " + // eurointegration
                "div.bpost, " + // blogs !!!
                "div.article, " + // istpravda life
                "div.tt4 > div.tt6, " + // champion
                "div.tt41 > div.tit3").first(); // champion
        if (subSite.equals("blogs.pravda.com.ua")) keyData = keyData.parent();
        if (subSite.equals("bzh.life")) keyData = body.select("div.layout").select("div.content.instruction").first();
        return keyData;
    }

    private Date resolveDate(Element dateElement, ChangableString outerContent)
    {
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
        Matcher universalMatcher = Pattern.compile("^(?:([0-9\\[\\]\\p{L}\\s,'№\"\\.\\p{Pd}]*?)(?:,?\\s))?" +
                "((?:[0-9]{2}\\.[0-9]{2}\\.[0-9]{4}(?:,?\\s[0-9]{2}:[0-9]{2})?)|" +
                "(?:(?:\\p{Lu}[\\p{Ll}']{5,8},\\s)?[0-9]{1,2}\\s\\p{L}{5,11},?\\s[0-9]{4}(?:,\\s[0-9]{2}:[0-9]{2})?))" +
                "(?:(?:,?\\s)?([0-9\\[\\]\\p{L}\\s,'№\"\\.\\p{Pd}]*?))?$").matcher(text);
        if(universalMatcher.find())
        {
            for(int i = 1; i < 4; i++)
            {
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
            col.writeToLog("timeString is null\nWhole text: " + text + "\nOuter content: " + outerContent);
            indicator = Indicator.PARSE_ERROR;
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
            col.writeToLog("Date can't be parsed. timeString:\n" + timeString);
            indicator = Indicator.PARSE_ERROR;
            return null;
        }
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
                indicator = Indicator.PARSE_ERROR;
                col.writeToLog("Neither ukrainian nor russian date format succeeded.");
            }
        }

        return d;
    }

    private void resolveAuthor(String containsAuthor)
    {
        System.out.println(containsAuthor);
    }

    private void acumulateData()
    {
        //for testing
        System.out.println(url);

        ChangableString outerContent = new ChangableString("");
        Elements inHead = doc.head().getElementsByTag("meta");
        subSite = url.getHost();
        setTitleAndType(inHead);
        Element body = doc.body();

        Element keyData = getKeyData(body);
        if(keyData == null)
        {
            indicator = Indicator.SELECTOR_ERROR;
            col.writeToLog("keyData is null, no elements found by query");
            return;
        }
        datePublished = resolveDate(keyData.select("div.post_news__date, " + //pravda
                "div.tit3, " +
                "table div.source, " + // tabloid
                "div.date1, span.dt2, " +
                "div.bpost > span.bdate, " +
                "div.dt1, " +
                "div.section-responsive div.article-date > span + span")
                .first(), outerContent);
        resolveAuthor(outerContent.toString());
    }

    public void filterImportant(String content)
    {
        //Matcher numComments = Pattern.compile("\\p{all}*?[Кк]омент[арі]");
        //if()
    }

    public String getTitle()
    {
        return title;
    }

    public Date getDatePublished()
    {
        return datePublished;
    }

    public String[] getTopics()
    {
        return topics;
    }

    public int getSeen()
    {
        return seen;
    }

    public int getCommented()
    {
        return commented;
    }

    public String getType()
    {
        return type;
    }

    public String getSubSite()
    {
        return subSite;
    }

    public String getAuthor()
    {
        return author;
    }

    public URL getURL()
    {
        return url;
    }
}

class ChangableString
{
    private String str;

    ChangableString(String s)
    {
        str = s;
    }

    String append(String what)
    {
        str += what;
        return str;
    }

    String set(String what)
    {
        str = what;
        return str;
    }

    public String toString()
    {
        return str;
    }


}