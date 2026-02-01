package searchengine.forkjoin;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import searchengine.model.Page;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;
import searchengine.services.PageIndexer;
import searchengine.services.SiteService;
import java.net.URI;
import java.util.List;
import java.util.concurrent.RecursiveAction;
import java.util.stream.Collectors;
import javax.net.ssl.*;
import java.security.cert.X509Certificate;



public class SiteIndexer extends RecursiveAction {

    private final String url;
    private final int siteId;
    private final String siteUrl;
    private final SiteService siteService;
    private final PageRepository pageRepository;
    private final SiteRepository siteRepository;
    private final PageIndexer pageIndexer;


    public SiteIndexer(String url, int siteId, String siteUrl, SiteService siteService,
                       PageRepository pageRepository, SiteRepository siteRepository, PageIndexer pageIndexer) {
        this.url = url;
        this.siteId = siteId;
        this.siteUrl = siteUrl;
        this.siteService = siteService;
        this.pageRepository = pageRepository;
        this.siteRepository = siteRepository;
        this.pageIndexer = pageIndexer;
    }

    @Override
    protected void compute() {
        if (siteService.isCanceled()) return;

        try {
            Thread.sleep(500 + (int) (Math.random() * 500));

            Connection.Response response = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .timeout(30000)
                    .ignoreHttpErrors(true)
                    .execute();

            Document doc = response.parse();
            int statusCode = response.statusCode();

            java.net.URI uri = new java.net.URI(response.url().toString());
            String finalPath = uri.getPath();
            if (finalPath == null || finalPath.isEmpty()) finalPath = "/";
            if (finalPath.length() > 1 && finalPath.endsWith("/")) {
                finalPath = finalPath.substring(0, finalPath.length() - 1);
            }


            Page savedPage = siteService.savePageIfNew(this.siteId, finalPath, statusCode, doc.html());

            if (statusCode == 200 && savedPage != null) {
                pageIndexer.indexPageContent(savedPage.getId());
            }

            System.out.println("INDEXED: " + finalPath + " [" + statusCode + "]");

            Elements links = doc.select("a[href]");
            String currentDomain = extractDomainName(this.siteUrl);

            List<SiteIndexer> tasksToLaunch = links.stream()
                    .map(link -> link.attr("abs:href"))
                    .filter(href -> href.startsWith(this.siteUrl)) // Ссылка должна быть внутри сайта
                    .map(href -> href.split("#")[0].split("\\?")[0]) // Убираем якоря и параметры
                    .map(href -> href.replaceAll("/+$", "")) // Убираем слэш в конце
                    .filter(href -> !isBinaryFile(href)) // Проверка на картинки/архивы
                    .filter(href -> siteService.addVisitedUrlFilter(href)) // Уникальность
                    .filter(href -> {
                        String targetPath = href.replace(this.siteUrl, "");
                        if (targetPath.isEmpty()) targetPath = "/";
                        return !siteService.isPageAlreadyIndexed(this.siteId, targetPath);
                    })
                    // Создаем новые задачи, передавая в них зависимости
                    .map(link -> new SiteIndexer(link, this.siteId, this.siteUrl,
                            siteService, pageRepository, siteRepository, pageIndexer))
                    .collect(Collectors.toList());

            if (!tasksToLaunch.isEmpty()) {
                invokeAll(tasksToLaunch);
            }

        } catch (Exception e) {
            System.err.println("STOPPED: " + url + " -> " + e.getMessage());
        }
    }



    private boolean isBinaryFile(String path) {
        path = path.toLowerCase();
        return path.endsWith(".pdf") || path.endsWith(".jpg") || path.endsWith(".png") ||
                path.endsWith(".gif") || path.endsWith(".zip") || path.endsWith(".eps") ||
                path.endsWith(".css") || path.endsWith(".js")  || path.endsWith(".svg");
    }
    private String extractDomainName(String url) {
        try {
            URI uri = new URI(url);
            String domain = uri.getHost();
            return domain.startsWith("www.") ? domain.substring(4) : domain;
        } catch (Exception e) {
            return url;
        }
    }
}
