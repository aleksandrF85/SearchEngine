package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.WebSite;

import java.util.Optional;

@Repository
public interface SiteRepository extends JpaRepository<WebSite, Integer> {

    Optional<WebSite> findByUrl(String url);

}
