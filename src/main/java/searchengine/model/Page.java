package searchengine.model;

import lombok.*;

import javax.persistence.*;
import javax.persistence.Index;
import java.util.List;
@NoArgsConstructor
@Getter
@ToString(exclude = "content")
@Setter
@Entity
@Table(name="page",
    indexes = @Index(name = "idx_page_path", columnList = "path", unique = false))
public class Page implements Comparable<Page>{

    /**
     * id INT NOT NULL AUTO_INCREMENT;
     * site_id INT NOT NULL — ID веб-сайта из таблицы site;
     * path TEXT NOT NULL — адрес страницы от корня сайта (должен начинаться со слэша, например: /news/372189/);
     * code INT NOT NULL — код HTTP-ответа, полученный при запросе страницы (например, 200, 404, 500 или другие);
     * content MEDIUMTEXT NOT NULL — контент страницы (HTML-код).
     *
     * По полю path должен быть установлен индекс, чтобы поиск по нему был быстрым, когда в нём будет много ссылок. Индексы рассмотрены в курсе «Язык запросов SQL».
     */

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="id", nullable = false)
    private int id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name="site_id", nullable=false, foreignKey = @ForeignKey(name = "pageOnSite"))
    private WebSite webSite;
    @Column(name="path", columnDefinition = "VARCHAR(750)", nullable = false) // Error Code: Specified key was too long; max key length is 3072 bytes
    private String path;

    @Column(name="code", nullable = false)
    private int code;

    @Column(name="content", columnDefinition = "MEDIUMTEXT", nullable = false)
    private String content;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy="page")
    private List<searchengine.model.Index> indexList;

    @Override
    public int compareTo(Page p) {

        return this.path.compareTo(p.path);
    }

}
