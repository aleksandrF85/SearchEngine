package searchengine.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.model.*;
import searchengine.model.searchModel.SearchLemma;
import searchengine.model.searchModel.SearchPage;
import searchengine.repository.LemmaRepository;
import searchengine.repository.SiteRepository;

import java.io.IOException;
import java.util.*;

@Service
public class SearchService {

    @Autowired
    LemmaRepository lemmaRepository;

    @Autowired
    SiteRepository siteRepository;

    public void findLemmas (String query, WebSite site){

        Map<String, Integer> lemmas;
        try {
            lemmas = LemmaFinder.getInstance().collectLemmas(query);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        ArrayList<Lemma> lemmaList = new ArrayList<>();
        for (Lemma lemma : lemmaRepository.findAll()) {
            if (lemmas.containsKey(lemma.getLemma()) && lemma.getFrequency() < 100 && lemma.getWebSite().equals(site)){
                    lemmaList.add(lemma);
            }
        }
        Collections.sort(lemmaList);

        List<SearchPage> searchPageList = getSearchPageList(lemmaList, getPageList(lemmaList));
        Collections.sort(searchPageList);

        for (SearchPage page :
                searchPageList) {
            //TODO создание объекта SearchData
        }

    }
    public List<Page> getPageList(List<Lemma> lemmaList){
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
                pagesList = shortList;
            }
        }
        return shortList;
    }

    public List<SearchPage> getSearchPageList(List<Lemma> lemmaList, List<Page> shortList){
        List<SearchPage> searchPageList = new ArrayList<>();
        if (shortList.size() != 0){
            for (Page p : shortList) {
                SearchPage page = new SearchPage();
                List<SearchLemma> searchLemmaList = new ArrayList<>();
                page.setPage(p);
                searchPageList.add(page);
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
            }
        }
        return searchPageList;
    }
    public void setRelevance(SearchPage page, List<SearchPage> searchPageList, List<SearchLemma> searchLemmaList){
        double relevanceAbs = 0;
        for (SearchLemma searchLemma :
                searchLemmaList) {
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

}
