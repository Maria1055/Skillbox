package searchengine.dto.search; // укажите ваш пакет

import lombok.Data;

@Data
public class SearchResultDto {
    private String site;
    private String siteName;
    private String uri;
    private String title;
    private String snippet;
    private Double relevance;
}
