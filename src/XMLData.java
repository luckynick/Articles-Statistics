import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

public class XMLData
{
    public static void main(String args[]) throws Exception
    {
        Document d = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
        Element root = d.createElement("titles_list");
        d.appendChild(root);
        Element title1 = d.createElement("title");
        title1.setTextContent("A baba halamaha spivala pisni");
        root.appendChild(title1);
        DOMSource ds = new DOMSource(d);
        StreamResult sr = new StreamResult("data.xml");
        TransformerFactory.newInstance().newTransformer().transform(ds, sr);
    }
}
