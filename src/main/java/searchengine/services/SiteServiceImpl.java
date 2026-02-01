
/*
package searchengine.services;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.connector.Response;
import org.jsoup.Connection;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import searchengine.config.SiteConfig;
import searchengine.config.SitesList;
import searchengine.dto.indexing.IndexingResponse;
import searchengine.forkjoin.SiteIndexer;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.model.Status;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicBoolean;
@Slf4j
@AllArgsConstructor
@Service
public class SiteServiceImpl implements SiteService {

    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final SitesList sitesList;

    private final AtomicBoolean indexingRunning = new AtomicBoolean(false);
    // Флаг для сигнализации об отмене
    private final AtomicBoolean isCanceled = new AtomicBoolean(false);

    public boolean isIndexingRunning() {
        return indexingRunning.get();
    }

@Override
    public IndexingResponse startIndexing() {
        if (!indexingRunning.compareAndSet(false, true)) {
            throw new IndexingCanceledException("Индексация уже запущена.");
        }
        isCanceled.set(false);

        // Запускаем весь процесс в отдельном потоке, чтобы не блокировать HTTP-запрос
        CompletableFuture.runAsync(() -> {
            try {
                startIndexingAllSites();
            } finally {
                // Гарантируем сброс флага после завершения асинхронного выполнения
                indexingRunning.set(false);
            }
        });

        return new IndexingResponse(true);
    }
@Override
    public IndexingResponse stopIndexing() {
        // Устанавливаем флаг отмены
        isCanceled.set(true);
        // Всегда возвращаем true, так как запрос на остановку принят
        return new IndexingResponse(true);
    }


    private void startIndexingAllSites() {
        List<SiteConfig> sitesToCrawl = sitesList.getSites();
        try {
            for ( SiteConfig siteConfig: sitesToCrawl) {
                // Проверяем отмену перед каждым новым сайтом
                if (isCanceled()) {
                    break;
                }
                indexSite(siteConfig.getUrl()); // Вызов приватного метода для одного сайта
            }
        } catch (IndexingCanceledException e) {
            // Обрабатываем исключение отмены, если оно было выброшено изнутри
            System.out.println(e.getMessage());
        }
    }


    @Transactional
    public void indexSite(String siteUrl) {
        Site site = null;

        try {
            siteRepository.findAll().stream()
                    .filter(s -> s.getUrl().equals(siteUrl))
                    .findFirst()
                    .ifPresent(existingSite -> {
                        pageRepository.deleteBySite(existingSite);
                        siteRepository.delete(existingSite);
                    });

            site = new Site();
            site.setUrl(siteUrl);
            site.setName(extractDomainName(siteUrl));
            site.setStatus(Status.INDEXING);
            site.setStatusTime(Instant.now());
            site = siteRepository.save(site);

            ForkJoinPool forkJoinPool = ForkJoinPool.commonPool();
            SiteIndexer task = new SiteIndexer(siteUrl, site, this, pageRepository);
            forkJoinPool.invoke(task);

            if (!isCanceled.get()) {
                updateSiteStatus(site.getId(), Status.INDEXED, null);
            }


        } catch (CancellationException e) {
            if (site != null) {
                updateSiteStatus(site.getId(), Status.FAILED, "Индексация отменена.");
            }
        } catch (Exception e) {
        }
    }

    private String extractDomainName(String url) {
        try {
            java.net.URI uri = new java.net.URI(url);
            String domain = uri.getHost();
            return domain.startsWith("www.") ? domain.substring(4) : domain;
        } catch (Exception e) {
            return url;
        }
    }
@Transactional
        public void updateSiteStatus ( int siteId, Status status, String error){
            Site site = siteRepository.findById(siteId).orElseThrow();
            site.setStatus(status);
            site.setStatusTime(Instant.now());
            site.setLastError(error);
            siteRepository.save(site);
        }
@Transactional
    public void savePageAndTouchSiteStatus(Site site, String path, int code, String content) {
        Page page = new Page();
        page.setSite(site);
        page.setPath(path);
        page.setCode(code);
        page.setContent(content);
        pageRepository.save(page);

        site.setStatusTime(Instant.now());
        siteRepository.save(site);
    }

@Override
    public boolean isCanceled() {
        return isCanceled.get();
    }

*/
package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import searchengine.config.SiteConfig;
import searchengine.config.SitesList;
import searchengine.dto.common.Response;
import searchengine.dto.indexing.IndexingResponse;
import searchengine.exception.BadRequestException;
import searchengine.forkjoin.SiteIndexer;
import searchengine.model.*;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
@RequiredArgsConstructor
@Service("indexingService")
public class SiteServiceImpl implements SiteService {


    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;
    private final PageIndexer pageIndexer;
    private final LemmaParser lemmaParser;
    private final SitesList sitesList;
    private final PlatformTransactionManager transactionManager;
    private SiteService self;
    private final AtomicBoolean indexingRunning = new AtomicBoolean(false);
    private final AtomicBoolean isCanceled = new AtomicBoolean(false);
    private final Set<String> visitedUrls = ConcurrentHashMap.newKeySet();
    private ForkJoinPool siteIndexingPool;


    @Override
    public IndexingResponse startIndexing() {
        if (isIndexingRunning()) {
            throw new BadRequestException("Индексация уже запущена"); // Используем твой ExceptionHandler
        }

        indexingRunning.set(true);
        isCanceled.set(false);
        visitedUrls.clear();

        CompletableFuture.runAsync(() -> {
            try {
                startIndexingAllSites(); // ВЫЗОВ НАПРЯМУЮ
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                indexingRunning.set(false);
            }
        });

        return new IndexingResponse(true);
    }
    private void startIndexingAllSites() {
        List<SiteConfig> sites = sitesList.getSites();
        for (SiteConfig siteConfig : sites) {
            if (isCanceled.get()) break;

            Site site = initializeSite(siteConfig.getUrl());
            indexSite(site.getUrl(), site.getId());
        }
    }


    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Site initializeSite(String siteUrl) {
        String name = sitesList.getSites().stream()
                .filter(s -> s.getUrl().equals(siteUrl))
                .findFirst()
                .map(SiteConfig::getName)
                .orElse("Unknown");

        Site site = siteRepository.findByUrl(siteUrl).orElseGet(() -> {
            Site newSite = new Site();
            newSite.setUrl(siteUrl);
            newSite.setName(name);
            return newSite;
        });
        if (site.getId() != null) {
            if (site.getStatus() == Status.INDEXING) {
                return site;
            }
            indexRepository.deleteByPageSite(site);
            lemmaRepository.deleteBySite(site);
            pageRepository.deleteBySite(site);
        }

        site.setStatus(Status.INDEXING);
        site.setLastError(null);
        site.setStatusTime(Instant.now());

        return siteRepository.save(site);
    }


    @Override
    @Transactional
    public Page savePageIfNew(int siteId, String path, int code, String content) {
        String cleanPath = path.contains("?") ? path.substring(0, path.indexOf("?")) : path;
        String normalizedPath = (cleanPath.length() > 1 && cleanPath.endsWith("/"))
                ? cleanPath.substring(0, cleanPath.length() - 1)
                : cleanPath;
        Site site = siteRepository.findById(siteId)
                .orElseThrow(() -> new RuntimeException("Site not found with id: " + siteId));

        Page page = pageRepository.findBySiteAndPath(site, normalizedPath)
                .orElseGet(() -> {
                    Page newPage = new Page();
                    newPage.setSite(site);
                    newPage.setPath(normalizedPath);
                    return newPage;
                });

        page.setCode(code);
        page.setContent(content);

        return pageRepository.save(page);
    }


    @Override
    public IndexingResponse stopIndexing() {
        if (!indexingRunning.get() && (siteIndexingPool == null || siteIndexingPool.isShutdown())) {
            return new IndexingResponse(false, "Индексация не запущена");
        }

        isCanceled.set(true);
        if (siteIndexingPool != null) {
            siteIndexingPool.shutdownNow();
            System.out.println("DEBUG: ForkJoinPool принудительно остановлен.");// Принудительно останавливаем потоки
        }
        indexingRunning.set(false);
        return new IndexingResponse(true);
    }


    public boolean isIndexingRunning() {
        return indexingRunning.get();
    }

    @Override
    public boolean isCanceled() {
        return isCanceled.get();
    }


    private void indexSite(String url, int siteId) {
        System.out.println("DEBUG: Начинаю индексацию сайта: " + url);

        Site site = siteRepository.findById(siteId).orElseThrow(() ->
                new RuntimeException("CRITICAL: Сайт не найден в БД. ID: " + siteId));

        ForkJoinPool pool = new ForkJoinPool(4);
        try {
            SiteIndexer indexer = new SiteIndexer(
                    url,
                    siteId,
                    url,
                    this,
                    pageRepository,
                    siteRepository,
                    pageIndexer
            );

            pool.invoke(indexer);

            if (!isCanceled.get()) {
                updateSiteStatus(siteId, Status.INDEXED, null);
            }
        } catch (Exception e) {
            System.err.println("ОШИБКА ИНДЕКСАЦИИ " + url + ": " + e.getMessage());
            updateSiteStatus(siteId, Status.FAILED, e.getMessage());
        } finally {
            pool.shutdown();
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateSiteStatus(int siteId, Status status, String error) {
        Site site = siteRepository.findById(siteId)
                .orElseThrow(() -> new RuntimeException("Site not found with id: " + siteId));

        site.setStatus(status);
        site.setStatusTime(Instant.now());

        if (error != null) {
            site.setLastError(error.length() > 250 ? error.substring(0, 250) : error);
        } else {
            site.setLastError(null);
        }

        siteRepository.save(site);
    }


    @Override
    public Response indexSinglePage(String url) {
        if (url == null || url.isBlank()) {
            throw new BadRequestException("URL не задан");
        }

        String rootUrl = sitesList.getSites().stream()
                .map(SiteConfig::getUrl)
                .filter(url::startsWith)
                .findFirst()
                .orElse(null);

        if (rootUrl == null) {
            throw new BadRequestException("Данная страница находится за пределами сайтов из конфигурации");
        }
        CompletableFuture.runAsync(() -> {
            try {
                Site site = siteRepository.findByUrl(rootUrl).orElseThrow();
                String path = url.replace(rootUrl, "");
                if (path.isEmpty()) path = "/";
                pageRepository.findBySiteAndPath(site, path).ifPresent(this::deletePageIndexes);
                processSinglePage(url, rootUrl);

            } catch (Exception e) {
                System.err.println("Ошибка при переиндексации страницы " + url + ": " + e.getMessage());
                e.printStackTrace();
            }
        });

        return new Response(true);
    }

    private void processSinglePage(String url, String rootUrl) {
        Site site = siteRepository.findByUrl(rootUrl).orElseGet(() -> {
            Site newSite = new Site();
            newSite.setUrl(rootUrl);
            newSite.setName(extractDomainName(rootUrl));
            newSite.setStatus(Status.INDEXED);
            newSite.setStatusTime(Instant.now());
            return siteRepository.save(newSite);
        });

        String path = url.replace(rootUrl, "");
        if (path.isEmpty()) path = "/";

        try {
            Optional<Page> existingPage = pageRepository.findBySiteAndPath(site, path);

            existingPage.ifPresent(this::deletePageIndexes);

            org.jsoup.Connection.Response response = org.jsoup.Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .timeout(15000)
                    .ignoreHttpErrors(true)
                    .proxy(java.net.Proxy.NO_PROXY)
                    .execute();

            String htmlContent = response.parse().html();

            savePageIfNew(site.getId(), path, response.statusCode(), htmlContent);


            pageRepository.findBySiteAndPath(site, path).ifPresent(p -> {
                pageIndexer.indexPageContent(p.getId());
            });

            System.out.println("DONE: Страница " + url + " переиндексирована.");

        } catch (java.io.IOException e) {
            System.err.println("Ошибка при переиндексации страницы " + url + ": " + e.getMessage());
        }
    }


    @Override
    public boolean addVisitedUrlFilter(String url) {
        return visitedUrls.add(url);
    }

    @Override
    public boolean isPageAlreadyIndexed(int siteId, String path) {
        return siteRepository.findById(siteId)
                .map(site -> pageRepository.existsBySiteAndPath(site, path))
                .orElse(false);
    }


    private String extractDomainName(String url) {
        try {
            java.net.URI uri = new java.net.URI(url);
            String domain = uri.getHost();
            return domain.startsWith("www.") ? domain.substring(4) : domain;
        } catch (Exception e) {
            return url;
        }
    }

    @Transactional
    public void deletePageIndexes(Page page) {
        List<IndexModel> pageIndexes = indexRepository.findByPage(page);
        if (pageIndexes.isEmpty()) return;

        List<Lemma> lemmasToUpdate = new ArrayList<>();
        List<Lemma> lemmasToDelete = new ArrayList<>();

        for (IndexModel index : pageIndexes) {
            Lemma lemma = index.getLemma();
            int currentFreq = lemma.getFrequency();

            if (currentFreq > 1) {
                lemma.setFrequency(currentFreq - 1);
                lemmasToUpdate.add(lemma);
            } else {
                lemmasToDelete.add(lemma);
            }
        }

        indexRepository.deleteAllInBatch(pageIndexes);

        if (!lemmasToUpdate.isEmpty()) {
            lemmaRepository.saveAll(lemmasToUpdate);
        }
        if (!lemmasToDelete.isEmpty()) {
            lemmaRepository.deleteAllInBatch(lemmasToDelete);
        }

    }

}