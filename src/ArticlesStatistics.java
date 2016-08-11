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
import java.util.Calendar;
import java.util.GregorianCalendar;


public class ArticlesStatistics
{
    static final String dataBasePath = "articles.txt";
    static final String statisticsPath = "statistics.txt";
    static final String logPath = "log.txt";

    volatile boolean execute = true;
    volatile int c = 0;


    public static void main(String args[]) throws Exception
    {
        ArticlesStatistics as = new ArticlesStatistics();

        try
        {
            new Thread(() -> {
                while(as.execute && as.c < 10)
                {
                    as.execute = false;
                    //as.backup(dataBasePath);

                    try
                    {
                        as.appendToDataBase(as.getNewTitles());
                    }
                    catch(SocketTimeoutException e)
                    {
                        as.execute = true;
                        as.writeToLog("WARNING: Try to read: " + as.c++ + ". Problem: " + e.toString());
                        System.out.println("WARNING: Try to read: " + as.c + ". Problem: " + e.toString());
                        try
                        {
                            Thread.sleep(10000);
                        }
                        catch (InterruptedException e1)
                        {
                            as.writeToLog(e1.toString());
                        }
                    }
                    catch (IOException e)
                    {
                        as.writeToLog(e.toString());
                    }
                }
            }).start();
        }
        catch (Exception e)
        {
            as.writeToLog(e.toString());
        }
        if(as.execute) as.writeToLog("ERROR: Failed to read new titles.");

        //UniversalDetector.main(new String[]{"C:\\Users\\User\\IdeaProjects\\Test\\" + dataBasePath});
    }

    String getNewTitles() throws IOException
    {
        Document doc;
        try
        {
            doc = Jsoup.parse(new URL("http://www.pravda.com.ua/"), 500);
        }
        catch (MalformedURLException e)
        {
            e.printStackTrace();
            return "";
        }
        doc.charset(Charset.forName("UTF-8"));
        Elements els = doc.getElementsByAttributeValueStarting("class", "news tab_item");
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
        System.out.println(c);
        writeToLog("INFO: Work result: " + c + " articles for now.");
    }

    void writeToLog(String what)
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

    void backup(String what) throws IOException
    {
        Path p = Paths.get(what);
        String parts[] = what.split("\\.");
        Path backupPath = Paths.get(parts[0] + "-backup." + parts[1]);
        if(Files.exists(backupPath)) Files.delete(backupPath);
        try
        {
            Files.copy(p, backupPath);
        }
        catch (IOException e)
        {
            e.printStackTrace();
            writeToLog("INFO: " + dataBasePath + "backed up.");
        }
    }
}
