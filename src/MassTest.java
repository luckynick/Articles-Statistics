import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MassTest
{

    public static void main(String args[]) throws IOException
    {
        BufferedWriter bw = new BufferedWriter(new FileWriter("afterBuilder.txt", true));

        TimeZone.setDefault(TimeZone.getTimeZone("Europe/Kiev"));
        boolean raw = true;
        Collector testCollector = new Collector(null, null)
        {
            @Override
            public void writeToLog(String what)
            {
                System.out.println("\u001B[31m" + what + "\u001B[0m");
            }
        };
        ArticleBuilder ab = new ArticleBuilder(testCollector);
        String testURL = "http://www.champion.com.ua/football/2016/09/12/664093/";
        String target = "http://www.pravda.com.ua/";

        for(int j = 0; j < 3; j++)
        {
            int iterations = 20;
            long sum = 0, min = 100000, max = 0, minus = 0;
            for(int i = 0; i < iterations; i++)
            {
                try
                {
                    if(raw) ab.massBuild(target);
                    else ab.build(testURL);
                }
                catch(SocketTimeoutException ex)
                {
                    testCollector.writeToLog(ex.getMessage());
                    iterations++;
                    minus++;
                    continue;
                }
                long current = ab.getArticlesExeTime();
                sum += current;
                if(min > current) min = current;
                if(max < current) max = current;
                bw.write(String.valueOf(current) + "\r\n");
            }
            System.out.println("Middle time: " + sum/(iterations - minus));
            bw.write("!!!" + sum / (iterations - minus) + "|" + min + "|" + max + "\r\n");
        }
        bw.close();
    }
}
