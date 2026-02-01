package searchengine.dto.search;

import lombok.EqualsAndHashCode;
import searchengine.dto.common.Response;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class SearchResponse extends Response {
    private int count;
    private List<SearchResultDto> data;


    public SearchResponse(boolean result, int count, List<SearchResultDto> data) {
        super(result);
        this.count = count;
        this.data = data;
    }

}
