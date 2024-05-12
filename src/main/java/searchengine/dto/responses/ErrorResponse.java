package searchengine.dto.responses;


import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class ErrorResponse {

    private boolean result;

    private String error;
}
