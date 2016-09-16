import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import javafx.util.Pair;


public class Article
{
    private String subSite;
    private boolean locked;
    //implement program's start time in controlling class
    private String title;

    private String description;
    private Date datePublished;
    private URL url;
    private String type;
    private String author;
    private int seen = -1, commented = -1;
    private long loadTime = -1; //in millisec
    private List<Pair<String, Integer>> social;
    private List<Pair<String, URL>> readAlso, topics;
    private Indicator indicator = Indicator.SUCCESS;

    public Article(String link) throws MalformedURLException
    {
        url = new URL(link);
        subSite = url.getHost();
    }

    public Article(String link, String description, Date datePublished, String type, String author, int seen, int commented,
                   long loadTime, List<Pair<String, URL>> readAlso, List<Pair<String, URL>> topics,
                   List<Pair<String, Integer>> social, Indicator indicator) throws MalformedURLException
    {
        url = new URL(link);
        subSite = url.getHost();
        this.description = description;
        this.datePublished = datePublished;
        this.type = type;
        this.author = author;
        this.seen = seen;
        this.commented = commented;
        this.loadTime = loadTime;
        this.readAlso = readAlso;
        this.topics = topics;
        this.social = social;
        this.indicator = indicator;
    }

    /**
     * Set whole object non-writable
     * @return
     */
    public void lock()
    {
        locked = true;
    }

    /**
     * Chack if object is non-writable
     * @return
     */
    public boolean isLocked()
    {
        return locked;
    }

    public String getTitle()
    {
        return title;
    }

    public String getDescription()
    {
        return description;
    }

    public Date getDatePublished()
    {
        return (Date)datePublished.clone();
    }

    public int getSeen()
    {
        return seen;
    }

    public List<Pair<String, URL>> getReadAlso()
    {
        return Collections.unmodifiableList(readAlso);
    }

    public List<Pair<String, URL>> getTopics()
    {
        return Collections.unmodifiableList(topics);
    }

    public List<Pair<String, Integer>> getSocial()
    {
        return Collections.unmodifiableList(social);
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

    public long getLoadTime()
    {
        return loadTime;
    }

    public Indicator getIndicator()
    {
        return indicator;
    }

    public boolean setIndicator(Indicator indicator)
    {
        if(locked)
        {
            System.out.println("Tried to write to locked article or assigned field.");
            return false;
        }
        this.indicator = indicator;
        return true;
    }

    public boolean setTopics(List<Pair<String, URL>> topics)
    {
        if(locked || topics != null)
        {
            System.out.println("Tried to write to locked article or assigned field.");
            return false;
        }
        this.topics = topics;
        return true;
    }

    public boolean setReadAlso(List<Pair<String, URL>> readAlso)
    {
        if(locked || readAlso != null)
        {
            System.out.println("Tried to write to locked article or assigned field.");
            return false;
        }
        this.readAlso = readAlso;
        return true;
    }

    public boolean setSocial(List<Pair<String, Integer>> social)
    {
        if(locked || social != null)
        {
            System.out.println("Tried to write to locked article or assigned field.");
            return false;
        }
        this.social = social;
        return true;
    }

    public boolean setLoadTime(long loadTime)
    {
        if(locked/* || loadTime != -1*/) //think about it
        {
            System.out.println("Tried to write to locked article or assigned field.");
            return false;
        }
        this.loadTime = loadTime;
        return true;
    }

    public boolean setCommented(int commented)
    {
        if(locked || commented != -1)
        {
            System.out.println("Tried to write to locked article or assigned field.");
            return false;
        }
        this.commented = commented;
        return true;
    }

    public boolean setSeen(int seen)
    {
        if(locked || seen != -1)
        {
            System.out.println("Tried to write to locked article or assigned field.");
            return false;
        }
        this.seen = seen;
        return true;
    }

    public boolean setAuthor(String author)
    {
        if(locked || (author != null && !author.equals("") && !author.equals("null")))
        {
            System.out.println("Tried to write to locked article or assigned field.");
            return false;
        }
        this.author = author;
        return true;
    }

    public boolean setType(String type)
    {
        if(locked)
        {
            System.out.println("Tried to write to locked article or assigned field.");
            return false;
        }
        this.type = type;
        return true;
    }

    public boolean setDatePublished(Date datePublished)
    {
        if(locked)
        {
            System.out.println("Tried to write to locked article or assigned field.");
            return false;
        }
        this.datePublished = datePublished;
        return true;
    }

    public boolean setDescription(String description)
    {
        if(locked)
        {
            System.out.println("Tried to write to locked article or assigned field.");
            return false;
        }
        this.description = description;
        return true;
    }

    public boolean setTitle(String title)
    {
        if(locked)
        {
            System.out.println("Tried to write to locked article or assigned field.");
            return false;
        }
        this.title = title;
        return true;
    }
}
