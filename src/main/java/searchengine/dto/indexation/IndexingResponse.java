package searchengine.dto.indexation;


import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class IndexingResponse {

    private boolean result;

    private String error;
}
