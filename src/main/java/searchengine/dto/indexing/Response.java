package searchengine.dto.indexing;
// проверьте ваш пакет

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class Response {
    private boolean result;
    private String error;

    public Response(boolean result) {
        this.result = result;
    }

    public Response(boolean result, String error) {
        this.result = result;
        this.error = error;
    }
}
