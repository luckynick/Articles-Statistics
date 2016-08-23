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

    public Collector col;

    private Document doc;

    private String title;
    private Date datePublished;
    private URL url;
    private String type;
    private String subSite;
    private String author;
    private String[] topics;
    private int seen, commented;
    private Map<String, Integer> social;

    @Deprecated
    public static void main(String args[]) throws Exception
    {
        String testURL = "http://tabloid.pravda.com.ua/watch4you/56f271b99d56c/"; //
        int duplicates = 0;
        long start = System.currentTimeMillis();
        if(true)
        {
            ArrayList<Article> al = new ArrayList<>();
            int success = 0, all = 0;
            String target = "http://www.pravda.com.ua/"; // http://www.epravda.com.ua/
            Document d = Jsoup.parse(new URL(target), 1000);
            Elements els = d.select("div.article a[href]" + args[0] + ", " + // pravda
                    "tr a[href], div[class^=post] a[href]"); // tabloid
            for (Element e : els)
            {
                all++;
                Article a = null;
                try
                {
                    String link;
                    String href = e.attr("href");
                    if (href.contains(target)) link = href;
                    else
                    {
                        if (href.contains("http://")) link = href;
                        else link = target + href.substring(1, href.length());
                    }
                    System.out.println(link);
                    a = new Article(new Collector(null, null)
                    {
                        @Override
                        public void writeToLog(String what)
                        {

                        }
                    }, link);
                    System.out.println(a.datePublished);
                    if(a.datePublished != null) success++;
                    if(urlArrayList.contains(a.url)) duplicates++;
                    urlArrayList.add(a.url);
                }
                catch (NullPointerException exc)
                {
                    System.out.println("\nNull pointer ex\n");
                    continue;
                }
//            if(a != null && !a.author.equals("null")) System.out.println(a.author);
                if(a.datePublished == null) al.add(a);
                System.out.println('\n');
            }
            System.out.println(success + " of " + all + " succeded.\n\n");
            System.out.println(duplicates + " duplicates");
            for(Article a : al)
            {
                System.out.println(a.url.toString() + "  " + a.title);
            }
        }
        else
        {
            Article a = new Article(new Collector(null, null)
            {
                @Override
                public void writeToLog(String what)
                {
                    System.out.println(what);
                }
            }, testURL);
            System.out.println(a.datePublished);
        }
        System.out.println(System.currentTimeMillis() - start + " milliseconds");
    }

    public Article(Collector ob, String url)
    {
        col = ob;
        try
        {
            this.url = new URL(url);
        }
        catch (MalformedURLException e)
        {
            e.printStackTrace();
            return;
        }
        try
        {
            Connection c = Jsoup.connect(url).userAgent("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_9_2)" +
                    " AppleWebKit/537.36 (KHTML, like Gecko) Chrome/33.0.1750.152 Safari/537.36");
            c.timeout(6000);
            doc = c.get();
        }
        catch(SocketTimeoutException e)
        {
            System.out.println("Socket time out");
            return;
        }
        catch (IOException e)
        {
            e.printStackTrace();
            return;
        }
        doc.charset(Charset.forName("UTF-8"));
        acumulateData();

    }

    private void acumulateData()
    {
        Elements inHead = doc.head().getElementsByTag("meta");
        subSite = url.getHost();
        setTitleAndType(inHead);
        Element body = doc.body();

        Element keyData = getKeyData(body);
        setDateTime(keyData);

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
        return keyData;
    }

    private void setDateTime(Element keyData)
    {
        datePublished = resolveDate(keyData.select("div.post_news__date, " + //pravda
                "div.tit3, " +
                "table div.source, " + // tabloid
                "div.date1, span.dt2, " +
                "div.bpost > span.bdate, " +
                "div.dt1")
                .first());
    }

    public Date resolveDate(Element dateElement)
    {
        Locale loc = Locale.forLanguageTag("uk-UA");
        String timeString = null;
        String outterContent = "";
        String text = dateElement.text().replaceAll("(?:[\\u00a0\\u0097])+", " ").replaceAll("\\s*?_\\s*?", ", ").
                replaceAll("[Вв]ерсія для друку\\s?", "");
        Matcher universalMatcher = Pattern.compile("^(?:([0-9\\[\\]\\p{L}\\s,\"\\.\\p{Pd}]*?)(?:,?\\s))?" +
                "((?:[0-9]{2}\\.[0-9]{2}\\.[0-9]{4}(?:,?\\s[0-9]{2}:[0-9]{2})?)|" +
                "(?:(?:\\p{Lu}[\\p{Ll}']{5,8},\\s)?[0-9]{1,2}\\s\\p{L}{5,11}\\s[0-9]{4},\\s[0-9]{2}:[0-9]{2}))" +
                "(?:(?:,?\\s)?([0-9\\[\\]\\p{L}\\s,\"\\.\\p{Pd}]*?))?$").matcher(text);
        //Matcher numComments = Pattern.compile("\\p{all}*?[Кк]омент[арі]");
        //if()
        int c = 1;
        if(universalMatcher.find())
        {
            for(int i = 1; i < 4; i++)
            {
                System.out.print(i + " group: ");
                System.out.println(universalMatcher.group(i));
                if(i == 2) timeString = universalMatcher.group(i);
                else outterContent += universalMatcher.group(i);
            }
        }
        if(timeString == null)
        {
            System.out.println("timeString is null");
            System.out.println(text);
            return null;
        }
        DateFormat df = null;
        if(timeString.matches("(?:\\p{Lu}[\\p{Ll}']{5,8},\\s)?[0-9]{1,2}\\s\\p{L}{2,11}?\\s[0-9]{4},\\s[0-9]{2}:[0-9]{2}"))
        {
            timeString = timeString.replaceFirst("\\p{Lu}[\\p{Ll}']{5,8},\\s", "");
            df = new SimpleDateFormat("d MMMM yyyy, HH:mm", loc);
        }
        else if(timeString.matches("[0-9]{2}\\.[0-9]{2}\\.[0-9]{4}")) df = new SimpleDateFormat("dd.MM.yyyy", loc);
        else if(timeString.matches("([0-9\\.]{7,10},?\\s[0-9]{2}:[0-9]{2})"))
        {
            timeString = timeString.replaceAll(",", "");
            df = new SimpleDateFormat("dd.MM.yyyy HH:mm", loc);
        }
        //else if(timeString.matches("[0-9]{2}\\s\\p{L}{2,11}?\\s[0-9]{4},\\s[0-9]{2}:[0-9]{2}")) df = new SimpleDateFormat("dd MMMM yyyy, HH:mm", loc);
        Date d = null;
        try
        {
            if(df == null)
            {
                System.out.println("Date couldn't be parsed.");
                System.out.println(timeString);
                return null;
            }
            d = df.parse(timeString);
        }
        catch (ParseException e)
        {
            e.printStackTrace();
        }
        return d;
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
