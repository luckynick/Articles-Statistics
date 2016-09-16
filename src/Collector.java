import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.*;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.util.*;


public class Collector
{
    final String dataBasePath;
    final String logPath;
    private Date dateStored = Calendar.getInstance().getTime();

    Collector(String dataBasePath, String logPath)
    {
        this.dataBasePath = dataBasePath;
        this.logPath = logPath;
    }

    volatile boolean execute = true;
    volatile int c = 0;

    /**
     * Start thread which tries to parse page, select titles and store them on top of data base
     *
     */
    public void collect()
    {
        Thread thr;
        try
        {
            thr = new Thread(() -> {
                while(execute && c < 10)
                {
                    execute = false;
                    try
                    {
//                        appendToDataBase(getNewTitles());
                        appendToDataBase(getNewURLs()); //use URLs in new version of program
                    }
                    catch(SocketTimeoutException e)
                    {
                        execute = true;
                        writeToLog("WARNING: Try to read: " + ++c + ". Problem: " + e.toString());
                        System.out.println("WARNING: Try to read: " + c + ". Problem: " + e.toString());
                        try
                        {
                            Thread.sleep(10000);
                        }
                        catch (InterruptedException e1)
                        {
                            writeToLog("ERROR: " + e1.toString());
                        }
                    }
                    catch (IOException e)
                    {
                        writeToLog("ERROR: " + e.toString());
                    }
                }
            });
            thr.start();
        }
        catch (Exception e)
        {
            writeToLog("ERROR: " + e.toString());
            return;
        }

        try
        {
            thr.join();
        }
        catch (InterruptedException e)
        {
            writeToLog("ERROR: " + e.toString());
        }
        if(execute) writeToLog("ERROR: Failed to read new titles (overtime)");
    }

    /**
     * Parse page and find titles of articles, collect them in String
     * @return titles as integer text
     * @throws IOException reasons - parse or encoding error
     */
    String getNewTitles() throws IOException
    {
        Document html;

        try
        {
            html = Jsoup.parse(new URL("http://www.pravda.com.ua/"), 1000);
        }
        catch (MalformedURLException e)
        {
            writeToLog(e.toString());
            return "";
        }
        html.charset(Charset.forName("UTF-8"));
        Elements els = html.getElementsByAttributeValueStarting("class", "news tab_item");
        String fromInet = new String("".getBytes(), "UTF-8");
        for(Element el : els)
        {
            Elements refs = el.getElementsByTag("a");
            for(Element ref : refs)
            {
                ref.select("em").remove();
                fromInet += ref.text() + "\r\n";
            }
        }
        return fromInet;
    }

    String getNewURLs() throws IOException
    {
        String result = "";
        ArrayList<String> allURLs = new ArrayList<>();
        String target = "http://www.pravda.com.ua/";
        Connection c = Jsoup.connect(target).userAgent("Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko)" +
                " Chrome/49.0.2623.87 Safari/537.36");
        c.timeout(1000);
        Document d = c.get();
        Elements els = d.select("div[class^=article] a[href], " + // pravda
                "tr a[href], div[class^=post] a[href]"); // tabloid
        for(Element e : els)
        {
            String link;
            String href = e.attr("href");
            if (href.contains(target)) link = href;
            else
            {
                if (href.contains("http://")) link = href;
                else link = target + href.substring(1, href.length());
            }
            if(!allURLs.contains(link)) allURLs.add(link); //eliminate duplicates
        }
        for(String link : allURLs)
        {
            result += link + "\r\n";
        }
        return result;
    }

    /**
     * Add new titles on top data base
     * @param newArticles new titles
     * @throws IOException in cases of reading/writing to file
     */
    void appendToDataBase(String newArticles) throws IOException
    {
        BufferedReader br;
        try
        {
            br = new BufferedReader(new InputStreamReader(new FileInputStream(dataBasePath), "UTF-8"));
        }
        catch (FileNotFoundException e)
        {
            writeToLog("ERROR: File " + dataBasePath + " not found.");
            return;
        }
        String inFile = "";
        String firstLine = br.readLine();
        inFile += firstLine + "\r\n";
        while(br.ready())
        {
            inFile += br.readLine() + "\r\n";
        }
        br.close();
        if(firstLine != null && !firstLine.equals(""))
        {
            int cutPos = newArticles.indexOf(firstLine);
            if(cutPos != -1)
            {
                newArticles = newArticles.substring(0, cutPos);
            }
        }
        BufferedWriter bw;
        try
        {
            bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(dataBasePath), "UTF-8"));
        }
        catch (FileNotFoundException e)
        {
            writeToLog("ERROR: File " + dataBasePath + " not found.");
            return;
        }
        if(newArticles == null) newArticles = "";
        bw.write(newArticles + inFile);
        bw.close();

        int c = 0;
        String s = newArticles + inFile;
        for(int i = 0; i < s.length(); i++)
        {
            if(s.charAt(i) == '\n') c++;
        }
        System.out.println(c + " titles");
        writeToLog("INFO: Work result: " + c + " titles for now.");
    }

    /**
     * Write comment of event to log file
     * @param what description of event
     */
    public void writeToLog(String what)
    {
        DecimalFormat df = new DecimalFormat("00");
        GregorianCalendar c = new GregorianCalendar();
        what = "(" + df.format((double)c.get(Calendar.DAY_OF_MONTH)) + "." +
                df.format((double)c.get(Calendar.MONTH)) + "." + c.get(Calendar.YEAR) + "  " +
                df.format((double)c.get(Calendar.HOUR_OF_DAY)) + ":" + df.format((double)c.get(Calendar.MINUTE)) +
                ":" + df.format((double)c.get(Calendar.SECOND)) + ") " + what;
        try
        {
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(logPath, true)));
            bw.write(what + "\r\n");
            bw.close();
        }
        catch (IOException e)
        {
            System.exit(-1);
        }
    }

    /**
     * Create copy of existing data base
     * @param what data base path
     */
    void backup(String what)
    {
        Path p = Paths.get(what);
        String parts[] = what.split("\\.");
        Path backupPath = Paths.get(parts[0] + "-backup." + parts[1]);
        if(Files.exists(backupPath)){
            try
            {
                Files.delete(backupPath);
            }
            catch (IOException e)
            {
                writeToLog("ERROR: " + e.toString());
            }
        }
        try
        {
            Files.copy(p, backupPath);
        }
        catch (IOException e)
        {
            writeToLog("ERROR: " + e.toString());
        }
        writeToLog("INFO: " + dataBasePath + "backed up.");
    }

    public Date getDateStored()
    {
        return dateStored;
    }
}
