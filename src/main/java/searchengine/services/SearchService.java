package searchengine.services;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.dto.search.SearchData;
import searchengine.dto.search.SearchResponse;
import searchengine.exeption.FailedSearchException;
import searchengine.model.Index;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.model.WebSite;
import searchengine.model.searchModel.SearchLemma;
import searchengine.model.searchModel.SearchPage;
import searchengine.repository.SiteRepository;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

@Slf4j
@Service
public class SearchService {

    @Autowired
    SiteRepository siteRepository;

    HtmlParser htmlParser;

    private double relevanceMax = 0;

    @SneakyThrows
    public SearchResponse startSearch(String query, String site, Integer offset, Integer limit){
        if (query.isEmpty()){
            throw new FailedSearchException("Задан пустой поисковый запрос!");
        }

        List<WebSite> searchList = new ArrayList<>();

        if(site.isEmpty()) {
            searchList.addAll(siteRepository.findAll());
        } else {
            URL url = getUrl(site);
            String home = url.getProtocol() + "://" + url.getHost().replace("www.", "");
            WebSite webSite = siteRepository.findByUrl(home).orElse(null);
            if (webSite != null) {
                searchList.add(webSite);
            } else {
                throw new FailedSearchException("Указанная страница не найдена");
            }
        }

        List<SearchData> searchData = new ArrayList<>();
        searchList.forEach(s -> searchData.addAll(searchWorker(query, s)));

        if (searchData.isEmpty()){
            throw new FailedSearchException("Совпадений не найдено!");
        }

        SearchResponse response = new SearchResponse();
        Collections.sort(searchData, Comparator.comparing(SearchData::getRelevance));
        Collections.reverse(searchData);
        response.setData(setSearchData(searchData, offset, limit));
        response.setCount(searchData.size());
        response.setResult(true);
        return response;
    }

    public URL getUrl(String path){
        URL url;
        try {
            url = new URL(path);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
        return url;
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
            searchData.setTitle(htmlParser.getTitle(page.getPage().getContent()));
            searchData.setRelevance(page.getRelevance());
            searchData.setSnippet(findPageSnippet(page, lemmaList));
            if (!searchData.getSnippet().isEmpty()) {
                searchDataList.add(searchData);
            }
        }
        return searchDataList;
    }

    public List<Lemma> findLemmas (String query, WebSite site){
        Set<String> queryLemmas = new HashSet<>();
        try {
            queryLemmas.addAll(LemmaFinder.getInstance().getLemmaSet(query));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        ArrayList<Lemma> lemmaList = new ArrayList<>();

        for (Lemma lemma : site.getLemmaList()) {
            if (queryLemmas.contains(lemma.getLemma()) && lemma.getFrequency() < 100 && lemma.getWebSite().equals(site)){
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
            searchPageList.add(page);
        }
        searchPageList.forEach(page -> countRelevance(searchPageList));
        return searchPageList;
    }

    public void countRelevance(List<SearchPage> searchPageList){

        for (SearchPage searchPage : searchPageList) {
            if (searchPage.getRelevanceAbs() > relevanceMax){
                relevanceMax = searchPage.getRelevanceAbs();
            }
        }
        for (SearchPage searchPage: searchPageList) {
            searchPage.setRelevance(searchPage.getRelevanceAbs() / relevanceMax);
        }
    }

    @SneakyThrows
    public String findPageSnippet(SearchPage page, List<Lemma> queryLemmas) {
        htmlParser = new HtmlParser();
        HashSet<String> words = LemmaFinder.getInstance().getMatches(page.getPage().getContent(), queryLemmas);
        HashSet<String> lines = new HashSet<>();
        String content = page.getPage().getContent();

        words.forEach(w -> lines.addAll(htmlParser.getOwnText(content, w)));
        Map<String, Integer> snippetMap = getSnippetMap(words, lines);
        if (snippetMap.isEmpty()){
            return "";
        }
        int maxValue = snippetMap.entrySet().stream()
                .max(Map.Entry.comparingByValue()).get().getValue();
        List<String> snippetList = new ArrayList<>();
        for (int i = maxValue; i > 0; i--){
            List<String> list = new ArrayList<>();
            int valueOfI = i;
            snippetMap.forEach((key, value) ->{
                if (value.equals(valueOfI)){
                    list.add(key);
                }
            });
            Collections.sort(list, Comparator.comparing(String::length));
            Collections.reverse(list);
            snippetList.addAll(list);
        }
        String snippet = snippetList.get(0);
        if (snippet.length() < 50 && snippetList.size() > 1) {
            return snippet + " ... " + snippetList.get(1);
        }
        return snippet;
    }

    public Map<String, Integer> getSnippetMap (HashSet<String> words, HashSet<String> lines){
        Map<String, Integer> snippetMap = new HashMap<>();
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
            if (count > 0) {
                snippetMap.put(line, count);
            }
        }
        return snippetMap;
    }
}
