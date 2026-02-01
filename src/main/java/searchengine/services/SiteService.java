package searchengine.services;


import searchengine.dto.common.Response;
import searchengine.dto.indexing.IndexingResponse;
import searchengine.model.Page;

public interface SiteService {
    IndexingResponse startIndexing();
    IndexingResponse stopIndexing();
    Response indexSinglePage(String url);

    boolean isCanceled();
    boolean addVisitedUrlFilter(String url);
    boolean isPageAlreadyIndexed(int siteId, String path);
    Page savePageIfNew(int siteId, String path, int code, String content);
    void updateSiteStatus(int siteId, searchengine.model.Status status, String error);
}
