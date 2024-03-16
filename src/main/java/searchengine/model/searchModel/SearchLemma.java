package searchengine.model.searchModel;

import lombok.Data;
import searchengine.model.Lemma;

@Data
public class SearchLemma {

    Lemma lemma;

    float rank;

}
