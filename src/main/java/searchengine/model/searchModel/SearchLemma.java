package searchengine.model.searchModel;

import lombok.Data;
import searchengine.model.Lemma;

@Data
public class SearchLemma {

    Lemma lemma;

    float rank;

    @Override
    public String toString() {
        return "SearchLemma{" +
                "lemma=" + lemma.getLemma() +
                ", rank=" + rank +
                '}';
    }
}
