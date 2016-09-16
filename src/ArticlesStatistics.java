import org.mozilla.universalchardet.UniversalDetector;

public class ArticlesStatistics
{
    static final String dataBasePath = "articles_urls.txt";
    static final String statisticsPath = "statistics.txt";
    static final String logPath = "log.txt";

    public static void main(String ... args) throws Exception
    {
        Collector col = new Collector(dataBasePath, logPath);
        col.collect();

        //col.backup("active\\articles.txt");
        //UniversalDetector.main(new String[]{"C:\\Users\\User\\IdeaProjects\\MassTest\\" + col.dataBasePath});
    }
}
