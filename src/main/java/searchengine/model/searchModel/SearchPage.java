package searchengine.model.searchModel;

import lombok.Data;
import searchengine.model.Page;

import java.util.List;

@Data
public class SearchPage {
    Page page;

    List<SearchLemma> lemmaRankList;

    double relevanceAbs;

    double relevance;

    public void setLemmaRankList(List<SearchLemma> lemmaRankList) {
        this.lemmaRankList = lemmaRankList;

        double sum = 0.0;
        for (SearchLemma l : lemmaRankList) {
            double rank = l.getRank();
            sum += rank;
        }
        setRelevanceAbs(sum);
    }

    @Override
    public String toString() {
        return "SearchPage{" +
                "page=" + page.getWebSite().getUrl() + page.getPath() +
                ", lemmaRankList=" + lemmaRankList +
                ", relevanceAbs=" + relevanceAbs +
                ", relevance=" + relevance +
                '}';
    }
}
