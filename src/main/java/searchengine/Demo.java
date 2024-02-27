package searchengine;

import lombok.SneakyThrows;
import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import searchengine.config.Site;
import searchengine.exeption.FailedIndexingException;
import searchengine.model.Page;
import searchengine.model.WebSite;
import searchengine.services.LemmaFinder;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Demo {
    public static void main(String[] args) throws IOException {
//        LuceneMorphology luceneMorph =
//                new RussianLuceneMorphology();
//        List<String> wordBaseForms =
//                luceneMorph.getNormalForms("леса");
//        wordBaseForms.forEach(System.out::println);
//
//        String text = "Повторное появление леопарда в Осетии позволяет предположить, " +
//                "что леопард постоянно обитает в некоторых районах Северного Кавказа.";
//
//        Map<String, Integer> lemmas= LemmaFinder.getInstance().collectLemmas(text);
//        lemmas.forEach((key, value) -> System.out.println(key + " - " + value));


        URL url = new URL("https://www.lenta.ru/news");

        String home = url.getProtocol() + "://" + url.getHost();
        String path = url.getFile();

        System.out.println(home);
        System.out.println(path);




    }
}
