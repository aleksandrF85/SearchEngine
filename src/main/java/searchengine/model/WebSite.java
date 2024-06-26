package searchengine.model;

import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@NoArgsConstructor
@ToString
@Getter
@Setter
@Entity
@Table(name="site")
public class WebSite {

    /**
     * id INT NOT NULL AUTO_INCREMENT;
     * status ENUM('INDEXING', 'INDEXED', 'FAILED') NOT NULL — текущий статус полной индексации сайта, отражающий готовность поискового движка осуществлять поиск по сайту — индексация или переиндексация в процессе, сайт полностью проиндексирован (готов к поиску) либо его не удалось проиндексировать (сайт не готов к поиску и не будет до устранения ошибок и перезапуска индексации);
     * status_time DATETIME NOT NULL — дата и время статуса (в случае статуса INDEXING дата и время должны обновляться регулярно при добавлении каждой новой страницы в индекс);
     * last_error TEXT — текст ошибки индексации или NULL, если её не было;
     * url VARCHAR(255) NOT NULL — адрес главной страницы сайта;
     * name VARCHAR(255) NOT NULL — имя сайта.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="id", nullable = false)
    private int id;
    @Enumerated(EnumType.STRING)
    @Column(name="status", columnDefinition = "ENUM('INDEXING', 'INDEXED', 'FAILED')", nullable = false)
    private Status status;

    @Column(name="status_time", columnDefinition = "DATETIME", nullable = false)
    @DateTimeFormat(pattern = "yyyy-MM-dd hh:mm:ss")
    private LocalDateTime statusTime;

    @Column(name="last_error", columnDefinition = "TEXT")
    private String lastError;

    @Column(name="url", columnDefinition = "VARCHAR(255)", nullable = false)
    private String url;

    @Column(name="name", columnDefinition = "VARCHAR(255)", nullable = false)
    private String name;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy="webSite")
    private List<Page> pages;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy="webSite")
    private List<Lemma> lemmaList;

}
