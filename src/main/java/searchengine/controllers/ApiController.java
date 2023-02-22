package searchengine.controllers;
import org.springframework.http.HttpStatus;
import searchengine.dto.indexing.Response;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import searchengine.dto.indexing.IndexingResponse;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.services.SearchService;
import searchengine.services.SiteService;
import searchengine.services.StatisticsService;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class ApiController {
    private final SiteService siteService;
    private final StatisticsService statisticsService;
    private final SearchService searchService;


    @Autowired
    public ApiController(@Lazy SiteService siteService, StatisticsService statisticsService, SearchService searchService) {
        this.siteService = siteService;
        this.statisticsService = statisticsService;
        this.searchService = searchService;
    }

    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> statistics() {
        return ResponseEntity.ok(statisticsService.getStatistics());
    }

    @RequestMapping(value = "/startIndexing", method = {RequestMethod.GET, RequestMethod.POST})
    public ResponseEntity<IndexingResponse> startIndexing() {
        return ResponseEntity.ok(siteService.startIndexing());
    }

    @RequestMapping(value = "/stopIndexing", method = {RequestMethod.GET, RequestMethod.POST})
    public ResponseEntity<IndexingResponse> stopIndexing() {
        return ResponseEntity.ok(siteService.stopIndexing());
    }

    @PostMapping("/indexPage")
    public ResponseEntity<Object> indexPage(@RequestParam String url) {
        if (url == null || url.isBlank()) {
            return ResponseEntity.ok(Map.of("result", false, "error", "URL не задан"));
        }

        // Добавляем очистку пробелов
        boolean result = siteService.indexSinglePage(url.trim());

        if (result) {
            return ResponseEntity.ok(Map.of("result", true));
        } else {
            // Сообщение должно СТРОГО соответствовать ТЗ, чтобы фронтенд его отобразил
            return ResponseEntity.ok(Map.of(
                    "result", false,
                    "error", "Данная страница находится за пределами сайтов, указанных в конфигурационном файле"
            ));
        }
    }

    @GetMapping("/search")
    public ResponseEntity<Object> search(
            @RequestParam(name = "query", required = false) String query,
            @RequestParam(name = "site", required = false) String site,
            @RequestParam(name = "offset", defaultValue = "0") Integer offset, // Integer лучше переносит отсутствие данных
            @RequestParam(name = "limit", defaultValue = "20") Integer limit) {

        if (query == null || query.isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("result", false, "error", "Задан пустой поисковый запрос"));
        }

        try {
            Object response = searchService.search(query, site, offset, limit);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            // Если поиск упал (например, леммы еще не проиндексированы)
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("result", false, "error", "Ошибка при выполнении поиска: " + e.getMessage()));
        }
    }

}
