package searchengine.model;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.util.List;
@Getter
@Setter
@Entity
@Table(name="lemma")
public class Lemma {


    /**
     * id INT NOT NULL AUTO_INCREMENT;
     * site_id INT NOT NULL — ID веб-сайта из таблицы site;
     * lemma VARCHAR(255) NOT NULL — нормальная форма слова (лемма);
     * frequency INT NOT NULL — количество страниц, на которых слово встречается хотя бы один раз. Максимальное значение не может превышать общее количество слов на сайте.
     */

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="id", nullable = false)
    private int id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name="site_id", nullable=false, foreignKey = @ForeignKey(name = "lemmaOnSite"))
    private WebSite webSite;

    @Column(name="lemma", columnDefinition = "VARCHAR(255)", nullable = false)
    private String lemma;

    @Column(name="frequency", nullable = false)
    private Integer frequency;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy="lemma")
    private List<Index> indexList;

}
