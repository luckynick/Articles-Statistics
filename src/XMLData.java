import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;

public class XMLData
{
    public static void main(String args[]) throws Exception
    {
        Article a1 = new Article(null, null);
        Article a2 = new Article(null, null);
        a1.main(new String[]{"", ""});
        ArrayList<URL> al1 = a1.urlArrayList;
        a1.urlArrayList = new ArrayList<>();
        a2.main(new String[]{"", ":only-child"});
        ArrayList<URL> al2 = a2.urlArrayList;
        for(Iterator<URL> it = al1.iterator(); it.hasNext(); )
        {
            URL temp = it.next();
            if(al2.contains(temp)) it.remove();
        }
        System.out.println("\n\nMissing articles:");
        for(URL url : al1)
        {
            System.out.println(url);
        }
        /*Document d = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
        Element root = d.createElement("titles_list");
        d.appendChild(root);
        Element title1 = d.createElement("title");
        title1.setTextContent("A baba halamaha spivala pisni");
        root.appendChild(title1);
        DOMSource ds = new DOMSource(d);
        StreamResult sr = new StreamResult("data.xml");
        TransformerFactory.newInstance().newTransformer().transform(ds, sr);*/
    }
}
