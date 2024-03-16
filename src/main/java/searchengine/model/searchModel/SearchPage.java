package searchengine.model.searchModel;

import lombok.Data;
import searchengine.model.Page;

import java.util.List;

@Data
public class SearchPage implements Comparable<SearchPage>{
    Page page;

    List<SearchLemma> lemmaRankList;

    double relevanceAbs;

    double relevance;


    @Override
    public int compareTo(SearchPage p) {
        return (int) (p.getRelevance() * 10 - this.relevance * 10);
    }
}
