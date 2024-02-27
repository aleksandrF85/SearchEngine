package searchengine.model;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
@Getter
@Setter
@Entity
@Table(name="search_index")
public class Index {

    /**
     * id INT NOT NULL AUTO_INCREMENT;
     * page_id INT NOT NULL — идентификатор страницы;
     * lemma_id INT NOT NULL — идентификатор леммы;
     * rank FLOAT NOT NULL — количество данной леммы для данной страницы.
     */

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="id", nullable = false)
    private int id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name="page_id", nullable=false, foreignKey = @ForeignKey(name = "pageId"))
    private Page page;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name="lemma_id", nullable=false, foreignKey = @ForeignKey(name = "lemmaId"))
    private Lemma lemma;

    @Column(name="lemma_rank", columnDefinition = "FLOAT", nullable = false)
    private Float rank;

}
