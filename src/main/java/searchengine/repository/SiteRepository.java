package searchengine.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.WebSite;

@Repository
public interface SiteRepository extends CrudRepository<WebSite, Integer> {

}
