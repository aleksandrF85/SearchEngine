package searchengine;

import com.github.demidko.aot.WordformMeaning;
import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import searchengine.services.LemmaFinder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.github.demidko.aot.WordformMeaning.lookupForMeanings;

public class Demo {
    public static void main(String[] args) throws IOException {
        LuceneMorphology luceneMorph =
                new RussianLuceneMorphology();
        List<String> wordBaseForms =
                luceneMorph.getNormalForms("леса");
        wordBaseForms.forEach(System.out::println);

        String text = "Повторное появление леопарда в Осетии позволяет предположить, " +
                "что леопард постоянно обитает в некоторых районах Северного Кавказа.";

        Map<String, Integer> lemmas= LemmaFinder.getInstance().collectLemmas(text);
        lemmas.forEach((key, value) -> System.out.println(key + " - " + value));


        var meanings = lookupForMeanings("люди");

        System.out.println(meanings.size());
        /* 1 */

        System.out.println(meanings.get(0).getMorphology());
        /* [С, мр, им, мн] */

        System.out.println(meanings.get(0).getLemma());
        /* человек */

        for (var t : meanings.get(0).getTransformations()) {
            System.out.println(t.toString() + " " + t.getMorphology());
            /*
             * человек [С, мр, им, ед]
             * человека [рд, С, мр, ед]
             * человеку [С, мр, ед, дт]
             * человека [С, мр, ед, вн]
             * человеком [тв, С, мр, ед]
             * человеке [С, мр, ед, пр]
             * люди [С, мр, им, мн]
             * людей [рд, С, мр, мн]
             * человек [рд, С, мр, мн]
             * людям [С, мр, мн, дт]
             * человекам [С, мр, мн, дт]
             * людей [С, мр, мн, вн]
             * людьми [тв, С, мр, мн]
             * человеками [тв, С, мр, мн]
             * людях [С, мр, мн, пр]
             * человеках [С, мр, мн, пр]
             */
        }

    }
}
