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


//        @SneakyThrows
//        public void startIndexingOne(String s){
//            URL url;
//            try {
//                url = new URL(s);
//            } catch (MalformedURLException e) {
//                throw new RuntimeException(e);
//            }
//            String home = url.getProtocol() + "://" + url.getHost();
//            String path = url.getFile();
//            if (path == null){
//                indexingWorker(home);
//            } else {
//                for (Page p : pageRepository.findAll()) {
//                    if (p.getPath().equals(path)) {
//                        pageRepository.delete(p);
//                        indexingOnePage(path, p.getWebSite());
//                    }
//                    else {
//                        throw new FailedIndexingException("This page is off-site specified in the configuration file");
//                    }
//                }
//            }
//        }
//
//
//        public void startIndexingAll(){
//            List<Site> sitesList = sites.getSites();
//            List<Thread> threads = new ArrayList<>();
//
//            for(int i = 0; i < sitesList.size(); i++) {
//                Site site = sitesList.get(i);
//                URL url;
//                try {
//                    url = new URL(site.getUrl());
//                } catch (MalformedURLException e) {
//                    throw new RuntimeException(e);
//                }
//                String home = url.getProtocol() + "://" + url.getHost();
//
//                threads.add(new Thread(() -> {
//                    indexingWorker(home);
//                }));
//            }
//            threads.forEach(Thread::start);
//            for (Thread thread : threads) {
//                try {
//                    thread.join();
//                } catch (InterruptedException e) {
//                    throw new RuntimeException(e);
//                }
//            }
//        }
//        public void indexingOnePage(String url, WebSite webSite){
//            Page page = new Page();
//            page.setWebSite(webSite);
//            page.setPath(url);
//            page.setContent(htmlParser.getContent(url));
//            page.setCode(htmlParser.getCode(url));
//
//            pageRepository.save(page);
//            saveLemmas(page);
//        }


    }
}
