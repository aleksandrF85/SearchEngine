package searchengine.services;

import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import searchengine.exeption.FailedIndexingException;
import searchengine.model.Page;
import searchengine.model.WebSite;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.RecursiveAction;

@Slf4j
public class PageRecursiveAction extends RecursiveAction {

    private Page page;

    private HtmlParser parser;
    @Setter
    static boolean stopIndexing;

    @Getter
    private ConcurrentSkipListSet<Page> children = new ConcurrentSkipListSet<>();

    private static CopyOnWriteArrayList linksPool = new CopyOnWriteArrayList();

    public PageRecursiveAction(Page page) {
        this.page = page;
        parser = new HtmlParser();
    }

    @SneakyThrows
    @Override
    protected void compute() {

        linksPool.add(page.getWebSite().getUrl());
        WebSite webSite = page.getWebSite();
        ConcurrentSkipListSet<String> links = parser.getLinks(webSite.getUrl());
        for (String link : links) {
            if (!linksPool.contains(link) && link.startsWith(webSite.getUrl())) {
                if (stopIndexing){
                    throw new FailedIndexingException("индексация остановлена");
                }
                linksPool.add(link);
                Page child = new Page();
                child.setPath(link.replace(webSite.getUrl(), ""));
                child.setWebSite(webSite);
                child.setContent(parser.getContent(link));
                child.setCode(parser.getCode(link));
                children.add(child);
                log.info(child.toString());
            }
        }
        List<PageRecursiveAction> taskList = new ArrayList<>();

        for (Page child : children) {
            PageRecursiveAction task = new PageRecursiveAction(child);
            task.fork();
            taskList.add(task);
        }
       for (PageRecursiveAction task : taskList) {
            task.join();
        }
    }
}