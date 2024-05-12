package searchengine.services;

import lombok.Getter;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;



import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentSkipListSet;

@Getter
public class HtmlParser {
    private ConcurrentSkipListSet<String> links;

    private boolean isLink(String link) {

        if(!link.contains("#")
            && !link.contains("?")
            && !link.contains(".pdf")){
            return true;
        } else {
            return false;
        }
    }
    public ConcurrentSkipListSet<String> getLinks(String url) {
        links = new ConcurrentSkipListSet<>();
        try {
            Thread.sleep(150);

            Document doc = Jsoup.connect(url).timeout(1200).ignoreHttpErrors(true).followRedirects(false).get();
            Elements elements = doc.select("a[href]");
            for(Element e: elements) {
                String link = e.attr("abs:href");

                if (isLink(link)) {
                    links.add(link);
                }
            }
        } catch (InterruptedException | IOException e) {
            throw new RuntimeException(e);
        }
        return links;
    }

    public String getTitle(String url){

        Document doc;
        try {
            doc = Jsoup.connect(url).ignoreHttpErrors(true).followRedirects(false).get();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return doc.title();
    }

    public String getContent(String url){

        Document doc;
        try {
            doc = Jsoup.connect(url).ignoreHttpErrors(true).followRedirects(false).get();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return doc.html();
    }

    public int getCode(String path){
        int code;
        try {
            URL url = new URL(path);

            HttpURLConnection connection = (HttpURLConnection)url.openConnection();
            connection.setRequestMethod("GET");
            connection.connect();
            code = connection.getResponseCode();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return code;
    }

    public List<String> getText(String url) {
        List<String> lines = new ArrayList<>();
        Document doc;
        try {
            doc = Jsoup.connect(url).ignoreHttpErrors(true).followRedirects(false).get();
            Elements elements = doc.select("section");
            for (Element e : elements) {
                lines.add(e.text());
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return lines;
    }
}
