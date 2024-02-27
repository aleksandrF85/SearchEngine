package searchengine.services;

import com.sun.xml.bind.v2.TODO;
import lombok.Getter;

import lombok.Setter;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.exeption.FailedIndexingException;
import searchengine.model.*;
import searchengine.repository.IndexRepository;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ForkJoinPool;
@Service
public class IndexingService {

    HtmlParser htmlParser;
    @Autowired
    SitesList sites;
    @Autowired
    PageRepository pageRepository;
    @Autowired
    SiteRepository siteRepository;
    @Autowired
    LemmaRepository lemmaRepository;
    @Autowired
    IndexRepository indexRepository;

    @SneakyThrows
    public void startIndexingOne(String s){
        URL url;
        try {
            url = new URL(s);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
        String home = url.getProtocol() + "://" + url.getHost();
        String path = url.getFile();
        if (path == null){
            indexingWorker(home.replace("www.", ""));
        } else {
            for (Page page : pageRepository.findAll()) {
                if (page.getPath().equals(path)) {
                    pageRepository.delete(page);
                    indexingOnePage(page);
                }
                else {
                    throw new FailedIndexingException("This page is off-site specified in the configuration file");
                }
            }
        }
    }

    public void startIndexingAll(){
        List<Site> sitesList = sites.getSites();
        List<Thread> threads = new ArrayList<>();

        for(int i = 0; i < sitesList.size(); i++) {
            Site site = sitesList.get(i);
            URL url;
            try {
                url = new URL(site.getUrl());
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
            String home = url.getProtocol() + "://" + url.getHost();

            threads.add(new Thread(() -> {
                indexingWorker(home);
            }));
        }
        threads.forEach(Thread::start);
        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }
    public void indexingOnePage(Page page){
        String url = page.getWebSite().getUrl() + page.getPath();
        page.setContent(htmlParser.getContent(url));
        page.setCode(htmlParser.getCode(url));

        ConcurrentSkipListSet<Page> pages = new ConcurrentSkipListSet<>();
        pages.add(page);
        for (Page p: pageRepository.findAll()) {
            if (p.getWebSite().equals(page.getWebSite())){
                pages.add(p);
            }
        }
        saveLemmas(pages);
    }
    public void indexingWorker(String url){

        htmlParser = new HtmlParser();
        WebSite webSite = new WebSite();
        webSite.setUrl(url); //TODO проверить передаваемые адреса сделать единообразно
        webSite.setName(url.replace("https://", ""));
        webSite.setStatus(Status.INDEXING);
        webSite.setStatusTime(LocalDateTime.now());
        deleteIfIndexed(url);
        siteRepository.save(webSite);

        try {
            WebSite correctNameSite = siteRepository.findById(webSite.getId()).get();
            correctNameSite.setName(htmlParser.getTitle(url.replace("www.", "")));
            siteRepository.save(correctNameSite);

            Page mainPage = new Page();
            mainPage.setWebSite(webSite);
            mainPage.setPath("/");
            mainPage.setContent(htmlParser.getContent(url));
            mainPage.setCode(htmlParser.getCode(url));
            PageRecursiveAction task = new PageRecursiveAction(mainPage);
            new ForkJoinPool(Runtime.getRuntime().availableProcessors()).invoke(task);
            if(task.getChildren().isEmpty()){
                throw new FailedIndexingException("Indexing error: site home page is unavailable");
            }

            pageRepository.saveAll(task.getChildren());
            saveLemmas(task.getChildren());

            WebSite indexedSite = siteRepository.findById(webSite.getId()).get();
            indexedSite.setStatusTime(LocalDateTime.now());
            indexedSite.setStatus(Status.INDEXED);
            siteRepository.save(indexedSite);
        } catch (Exception e) {
            WebSite failedSite = siteRepository.findById(webSite.getId()).get();
            failedSite.setStatusTime(LocalDateTime.now());
//            StringWriter sw = new StringWriter();
//            PrintWriter pw = new PrintWriter(sw);
//            e.printStackTrace(pw);
//            failedSite.setLastError(sw.toString());
            failedSite.setLastError(e.getMessage());
            failedSite.setStatus(Status.FAILED);
            siteRepository.save(failedSite);
        }
    }

    public void deleteIfIndexed(String url){
        for (WebSite s :
                siteRepository.findAll()) {
            if(s.getUrl().equals(url)){
                siteRepository.delete(s);
            }
        }
    }

    public boolean isIndexingInProgress(){
        for (WebSite s :
                siteRepository.findAll()) {
            if (s.getStatus().equals(Status.INDEXING)) {
                return true;
            }
        }
        return false;
    }


    public void saveLemmas(ConcurrentSkipListSet<Page> pages){
        Map<String, Lemma> lemmaMap = new TreeMap<>();
        List<Index> indexList = new ArrayList<>();
        for (Page page :
                pages) {
            String text = page.getContent();

            Map<String, Integer> lemmas;
            try {
                lemmas = LemmaFinder.getInstance().collectLemmas(text);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            for (Map.Entry<String, Integer> entry : lemmas.entrySet()) {
                String key = entry.getKey();
                Integer value = entry.getValue();
                Lemma lemma = new Lemma();
                lemma.setWebSite(page.getWebSite());
                lemma.setLemma(key);
                lemma.setFrequency(1);
                if (lemmaMap.isEmpty() || !lemmaMap.containsKey(key)) {
                    lemmaMap.put(key, lemma);
                    Index index = new Index();
                    index.setPage(page);
                    index.setLemma(lemma);
                    index.setRank(Float.valueOf(value));
                    indexList.add(index);
                } else {
                    lemmaMap.get(key).setFrequency(lemmaMap.get(key).getFrequency() + 1);
                    Index index = new Index();
                    index.setPage(page);
                    index.setLemma(lemmaMap.get(key));
                    index.setRank(Float.valueOf(value));
                    indexList.add(index);
                }
            }
        }
        lemmaRepository.saveAll(lemmaMap.values());
        indexRepository.saveAll(indexList);
    }
}
