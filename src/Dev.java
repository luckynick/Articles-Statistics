import java.io.IOException;

import java.util.TimeZone;

public class Dev
{
    public static void main(String args[])
    {


        TimeZone.setDefault(TimeZone.getTimeZone("Europe/Kiev"));
        Collector testCollector = new Collector(null, null)
        {
            @Override
            public void writeToLog(String what)
            {
                System.out.println("\u001B[31m" + what + "\u001B[0m");
            }
        };
        ArticleBuilder ab = new ArticleBuilder(testCollector);
        boolean raw = false;
        String testURL = "http://www.eurointegration.com.ua/articles/2016/09/17/7054701/"; //http://www.istpravda.com.ua/short/2016/09/15/149197/
        String target = "http://www.pravda.com.ua/";
        try
        {
            if(raw) ab.massBuild(target);
            else ab.build(testURL);
        }
        catch(IOException ex)
        {
            testCollector.writeToLog(ex.getMessage());
        }

        System.out.println("Program executed in " + ab.getArticlesExeTime() + " milliseconds");
    }
}
