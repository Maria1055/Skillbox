package searchengine.dto.indexing;

import lombok.*;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class IndexingResponse extends Response {


    public IndexingResponse(boolean result) {
        super(result);
    }

    public IndexingResponse(boolean result, String error) {
        super(result, error);
    }

}