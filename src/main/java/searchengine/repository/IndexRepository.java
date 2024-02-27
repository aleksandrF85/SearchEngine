package searchengine.repository;

import org.springframework.data.repository.CrudRepository;
import searchengine.model.Index;

public interface IndexRepository extends CrudRepository<Index, Integer> {
}
