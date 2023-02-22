package searchengine.dto.search;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SearchResponse {
    private boolean result;
    private int count;
    private List<SearchResultDto> data;
    private String error;


    public SearchResponse(boolean result, int count, List<SearchResultDto> data) {
        this.result = result;
        this.count = count;
        this.data = data;
    }

}
