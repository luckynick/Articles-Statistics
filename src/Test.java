import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebClientOptions;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;

public class Test
{
    public static void main(String args[]) throws IOException
    {
        WebClient wClient = new WebClient(BrowserVersion.CHROME);
        WebClientOptions ops = wClient.getOptions();
        wClient.setJavaScriptTimeout(10000);
        ops.setTimeout(10000);
        ops.setJavaScriptEnabled(true);
        ops.setThrowExceptionOnScriptError(false);
        ops.setThrowExceptionOnFailingStatusCode(false);
        HtmlPage page = wClient.getPage("http://www.eurointegration.com.ua/articles/2016/09/17/7054701/");
        Document d = Jsoup.parse(page.getWebResponse().getContentAsString());
        System.out.println(d.select("div.block0").first().outerHtml());
        System.out.println(d.select("span.fb_comments_count").first().text());
        //Document document = Jsoup.parse(page.)
    }
}
