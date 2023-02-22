package searchengine.services;

import searchengine.dto.indexing.IndexingResponse;
import searchengine.model.Site;
import searchengine.model.Status;

public interface SiteService {

    IndexingResponse startIndexing();
    boolean addVisitedUrlFilter(String url);
    boolean isPageAlreadyIndexed(int siteId, String path);
    IndexingResponse stopIndexing();
    boolean indexSinglePage(String url);
    boolean isIndexingRunning();
    boolean isCanceled();
    void startIndexingAllSites();
    void indexSite(String siteUrl, int siteId);
    Site initializeSite(String siteUrl);
    void savePageIfNew(int siteId, String path, int code, String content);
    void indexPageContent(Integer pageId);
    void updateSiteStatus(int siteId, Status status, String error);
}
