package searchengine.services;

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
            String home = url.getProtocol() + "://" + url.getHost().replace("www.", "");

            threads.add(new Thread(() -> indexingWorker(home)));
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

    @SneakyThrows
    public void startIndexingOne(String s){
        boolean match = false;
        URL url;
        try {
            url = new URL(s);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
        String home = url.getProtocol() + "://" + url.getHost().replace("www.", "");
        String path = url.getFile();
        if (path.isEmpty()){
            match = true;
            indexingWorker(home);
        } else {
            for (Page page : pageRepository.findAll()) {
                if (page.getPath().equals(path)) {
                    match = true;
                    pageRepository.delete(page);
                    indexingOnePage(page);
                }
            }
        }
        if (!match){
            throw new FailedIndexingException("Данная страница находится за пределами сайтов, указанных в конфигурационном файле");
        }
    }

    public void indexingOnePage(Page page){
        String url = page.getWebSite().getUrl() + page.getPath();
        Page newPage = new Page();
        newPage.setContent(htmlParser.getContent(url));
        newPage.setCode(htmlParser.getCode(url));
        newPage.setPath(page.getPath());
        newPage.setWebSite(page.getWebSite());
        pageRepository.save(newPage);

        ConcurrentSkipListSet<Page> pages = new ConcurrentSkipListSet<>();
        for (Page p: pageRepository.findAll()) {
            if (p.getWebSite().equals(page.getWebSite())){
                pages.add(p);
            }
        }
        saveLemmas(pages);
    }
    public void indexingWorker(String url){
        htmlParser = new HtmlParser();

        WebSite site = saveSite(url);

        try {
            Page mainPage = new Page();
            mainPage.setWebSite(site);
            mainPage.setPath("/");
            mainPage.setContent(htmlParser.getContent(url));
            mainPage.setCode(htmlParser.getCode(url));
            PageRecursiveAction task = new PageRecursiveAction(mainPage);
            new ForkJoinPool(Runtime.getRuntime().availableProcessors()).invoke(task);
            if(task.getChildren().isEmpty()){
                throw new FailedIndexingException("главная страница сайта недоступна");
            }
            pageRepository.saveAll(task.getChildren());
            saveLemmas(task.getChildren());

            WebSite indexedSite = siteRepository.findById(site.getId()).get();
            indexedSite.setStatusTime(LocalDateTime.now());
            indexedSite.setStatus(Status.INDEXED);
            siteRepository.save(indexedSite);
        } catch (Exception e) {
            WebSite failedSite = siteRepository.findById(site.getId()).get();
            failedSite.setStatusTime(LocalDateTime.now());
            failedSite.setLastError("Ошибка индексации: " + e.getMessage());
            failedSite.setStatus(Status.FAILED);
            siteRepository.save(failedSite);
        }
    }

    public WebSite saveSite (String url){
        htmlParser = new HtmlParser();

        deleteIfIndexed(url);
        WebSite site = new WebSite();
        site.setUrl(url);
        site.setStatus(Status.INDEXING);
        site.setStatusTime(LocalDateTime.now());
        try {
            site.setName(htmlParser.getTitle(url));
        } catch (Exception e) {
            site.setName(url.replace("https://", ""));
            site.setStatus(Status.FAILED);
            site.setStatusTime(LocalDateTime.now());
            site.setLastError("Ошибка индексации: " + e.getMessage());
            siteRepository.save(site);
        }
        siteRepository.save(site);
        return site;
    }
    public void deleteIfIndexed(String url){
        for (WebSite site :
                siteRepository.findAll()) {
            if(site.getUrl().equals(url)){
                siteRepository.delete(site);
            }
        }
    }

    public boolean isIndexingInProgress(){
        for (WebSite site :
                siteRepository.findAll()) {
            if (site.getStatus().equals(Status.INDEXING)) {
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
