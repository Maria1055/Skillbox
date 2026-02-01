package searchengine.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import searchengine.dto.common.Response;
import searchengine.dto.indexing.IndexingResponse;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.services.SearchService;
import searchengine.services.SiteService;
import searchengine.services.StatisticsService;

import java.util.Map;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api")
public class ApiController {
    private final SiteService siteService;
    private final StatisticsService statisticsService;
    private final SearchService searchService;


    @GetMapping("/statistics")
    public StatisticsResponse statistics() {
        return statisticsService.getStatistics();
    }

    @GetMapping(value = "/startIndexing")
    public IndexingResponse startIndexing() {
        return siteService.startIndexing();
    }

    @GetMapping(value = "/stopIndexing")
    public IndexingResponse stopIndexing() {
        return siteService.stopIndexing();
    }

    @PostMapping("/indexPage")
    public Response indexPage (@RequestParam String url) {
        return siteService.indexSinglePage(url);
    }

    @GetMapping("/search")
    public Object search(
            @RequestParam(name = "query", required = false) String query,
            @RequestParam(name = "site", required = false) String site,
            @RequestParam(name = "offset", defaultValue = "0") Integer offset,
            @RequestParam(name = "limit", defaultValue = "20") Integer limit) {

            return searchService.search(query, site, offset, limit);
        }

}
