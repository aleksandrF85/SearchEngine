package searchengine.services;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.dto.search.SearchData;
import searchengine.dto.search.SearchResponse;
import searchengine.exeption.FailedIndexingException;
import searchengine.exeption.FailedSearchException;
import searchengine.model.Index;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.model.WebSite;
import searchengine.model.searchModel.SearchLemma;
import searchengine.model.searchModel.SearchPage;
import searchengine.repository.LemmaRepository;
import searchengine.repository.SiteRepository;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

@Slf4j
@Service
public class SearchService {

    @Autowired
    LemmaRepository lemmaRepository;

    @Autowired
    SiteRepository siteRepository;

    HtmlParser htmlParser;

    @SneakyThrows
    public SearchResponse startSearch(String query, String site, Integer offset, Integer limit){
        if (query.isEmpty()){
            throw new FailedSearchException("Задан пустой поисковый запрос!");
        }
        SearchResponse response = new SearchResponse();
        if(site.isEmpty()){
            List<SearchData> searchData = new ArrayList<>();
            for (WebSite s : siteRepository.findAll()) {
                searchData.addAll(searchWorker(query, s));
            }
                if (searchData.isEmpty()){
                    throw new FailedSearchException("Совпадений не найдено!");
                }
            response.setData(setSearchData(searchData, offset, limit));
            response.setCount(searchData.size());
            response.setResult(true);
            return response;
        }

        URL url;
        try {
            url = new URL(site);
        } catch (MalformedURLException e) {
            throw new FailedIndexingException("Некорректный адрес страницы!");
        }
        String home = url.getProtocol() + "://" + url.getHost().replace("www.", "");
        WebSite webSite = siteRepository.findByUrl(home).orElse(null);
        if (webSite != null) {
            List<SearchData> searchData = searchWorker(query, webSite);
            response.setData(setSearchData(searchData, offset, limit));
            response.setCount(searchData.size());
            response.setResult(true);
            return response;
        } else {
            throw new FailedSearchException("Указанная страница не найдена");
        }
    }

    public List<SearchData> setSearchData(List<SearchData> searchData, int offset, int limit){

        if (searchData.size() >= offset + limit) {
            List<SearchData> searchDataLimited = new ArrayList<>();
            for (int i = 0; i <= searchData.size(); i++){
                if (i >= offset && i < (offset + limit)){
                    searchDataLimited.add(searchData.get(i));
                }
            }
            return searchDataLimited;
        } else if (searchData.size() - limit >= 0){
            int start = searchData.size() - limit;
            List<SearchData> searchDataLimited = new ArrayList<>();
            for (int i = 0; i <= searchData.size(); i++){
                if (i == start && i < (start + limit)){
                    searchDataLimited.add(searchData.get(i));
                }
            }
            return searchDataLimited;
        }
        return searchData;
    }
    public List<SearchData> searchWorker(String query, WebSite site){
        htmlParser = new HtmlParser();

        List<Lemma> lemmaList = findLemmas(query, site);
        List<SearchPage> searchPageList = getSearchPageList(lemmaList, getPagesWithLemmas(lemmaList));
        List<SearchData> searchDataList = new ArrayList<>();

        for (SearchPage page : searchPageList) {
            SearchData searchData = new SearchData();
            searchData.setUri(page.getPage().getPath());
            searchData.setSite(page.getPage().getWebSite().getUrl());
            searchData.setSiteName(page.getPage().getWebSite().getName());
            String path = searchData.getSite() + searchData.getUri();
            searchData.setTitle(htmlParser.getTitle(path));
            searchData.setRelevance(page.getRelevance());
            searchData.setSnippet(findPageSnippet(page, lemmaList));
            searchDataList.add(searchData);
        }
        return searchDataList;
    }

    public List<Lemma> findLemmas (String query, WebSite site){
        Set<String> lemmas = new HashSet<>();
        try {
            lemmas.addAll(LemmaFinder.getInstance().getLemmaSet(query));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        ArrayList<Lemma> lemmaList = new ArrayList<>();
        for (Lemma lemma : lemmaRepository.findAll()) {
            if (lemmas.contains(lemma.getLemma()) && lemma.getFrequency() < 100 && lemma.getWebSite().equals(site)){
                    lemmaList.add(lemma);
            }
        }
        Collections.sort(lemmaList);
        return lemmaList;
    }

    public List<Page> getPagesWithLemmas(List<Lemma> lemmaList){
        ArrayList<Page> pagesList = new ArrayList<>();
        ArrayList<Page> shortList = new ArrayList<>();
        for (int i = 0; i < lemmaList.size(); i++){
            for (Index index: lemmaList.get(i).getIndexList()) {
                if (i == 0) {
                    pagesList.add(index.getPage());
                } else if (pagesList.contains(index.getPage())) {
                    shortList.add(index.getPage());
                }
            }
            if (!shortList.isEmpty()){
                pagesList.clear();
                pagesList.addAll(shortList);
                shortList.clear();
            }
        }
        return pagesList;
    }

    public List<SearchPage> getSearchPageList(List<Lemma> lemmaList, List<Page> shortList){
        List<SearchPage> searchPageList = new ArrayList<>();
        if (shortList.isEmpty()){
            return searchPageList;
        }
        for (Page p : shortList) {
            SearchPage page = new SearchPage();
            List<SearchLemma> searchLemmaList = new ArrayList<>();
            page.setPage(p);
            for (Lemma l : lemmaList){
                SearchLemma lemma = new SearchLemma();
                lemma.setLemma(l);
                for (Index i : p.getIndexList()) {
                    if(i.getLemma().equals(l)){
                        lemma.setRank(i.getRank());
                    }
                }
                searchLemmaList.add(lemma);
            }
            page.setLemmaRankList(searchLemmaList);
            setRelevance(page, searchPageList, searchLemmaList);
            searchPageList.add(page);
        }
        Collections.sort(searchPageList);
        return searchPageList;
    }
    public void setRelevance(SearchPage page, List<SearchPage> searchPageList, List<SearchLemma> searchLemmaList){
        double relevanceAbs = 0;
        for (SearchLemma searchLemma : searchLemmaList) {
            relevanceAbs += searchLemma.getRank();
        }
        page.setRelevanceAbs(relevanceAbs);

        double relevanceMax = 0;
        for (SearchPage searchPage : searchPageList) {
            if (searchPage.getRelevanceAbs() > relevanceMax){
                relevanceMax = searchPage.getRelevanceAbs();
            }
        }
        for (SearchPage searchPage: searchPageList) {
            double relevance = searchPage.getRelevanceAbs() / relevanceMax;
            page.setRelevance(relevance);
        }
    }

    @SneakyThrows
    public String findPageSnippet(SearchPage page, List<Lemma> queryLemmas) {
        htmlParser = new HtmlParser();
        Map<String, Integer> snippetMap = new HashMap<>();
        HashSet<String> words = LemmaFinder.getInstance().getMatches(page.getPage().getContent(), queryLemmas);
        HashSet<String> lines = new HashSet<>();
        String content = page.getPage().getContent();

        lines.add(htmlParser.getDescription(content));
        for (String word : words){
            lines.addAll(htmlParser.getOwnText(content, word));
        }

        for (String line : lines) {
            int count = 0;
            for (String word : words) {
                if (line.toLowerCase().contains(word)) {
                    int start = line.toLowerCase().indexOf(word);
                    int beginSnippet = start > 150 ? line.toLowerCase().indexOf(" ", start - 150) : 0;
                    int endSnippet = Math.min(start + 150, (line.length()));
                    String boldWord = line.substring(start, Math.max(start + word.length(), line.indexOf(" ", start))).replaceAll("\\p{Punct}", "");
                    if (!line.contains("<b>" + boldWord)) {
                        line = line.substring(beginSnippet, endSnippet).replaceAll(boldWord, "<b>" + boldWord + "</b>");
                        count = count + 1;
                    }
                }
            }

            if (count > 0){
                snippetMap.put(line, count);
            }
        }
        int maxValue = 0;
        String out = "Совпадений не найдено";
        for (Map.Entry<String, Integer> entry : snippetMap.entrySet()) {
            int i = entry.getValue();
            if (i > maxValue){
                maxValue = i;
                out = entry.getKey();
            }
        }
        return out;
    }
}
