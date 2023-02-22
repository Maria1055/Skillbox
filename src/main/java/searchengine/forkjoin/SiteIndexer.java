package searchengine.forkjoin;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;
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


    public SiteIndexer(String url, int siteId, String siteUrl, SiteService siteService,
                       PageRepository pageRepository, SiteRepository siteRepository) {
        this.url = url;
        this.siteId = siteId;
        this.siteUrl = siteUrl;
        this.siteService = siteService;
        this.pageRepository = pageRepository;
        this.siteRepository = siteRepository;
    }


    @Override
    protected void compute() {
        String htmlContent = null;
        int statusCode = 0;
        String finalPath = null;
        Document doc = null;

        try {
            Thread.sleep(500 + (int) (Math.random() * 500));
            if (siteService.isCanceled()) return;
            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, new TrustManager[]{new X509TrustManager() {
                public X509Certificate[] getAcceptedIssuers() { return null; }
                public void checkClientTrusted(X509Certificate[] certs, String t) {}
                public void checkServerTrusted(X509Certificate[] certs, String t) {}
            }}, new java.security.SecureRandom());

            Connection.Response response = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .timeout(40000)
                    .ignoreHttpErrors(true)
                    .proxy(java.net.Proxy.NO_PROXY)
                    .sslSocketFactory(sc.getSocketFactory())
                    .execute();

            doc = response.parse();
            htmlContent = doc.html();
            statusCode = response.statusCode();

            java.net.URI uri = new java.net.URI(response.url().toString());
            String tempPath = uri.getPath();
            if (tempPath == null || tempPath.isEmpty()) tempPath = "/";

            if (tempPath.length() > 1 && tempPath.endsWith("/")) {
                tempPath = tempPath.substring(0, tempPath.length() - 1);
            }
            finalPath = tempPath;

        } catch (Exception e) {
            System.err.println("ERROR: " + url + " -> " + e.getMessage());
            return;
        }

        if (htmlContent != null && finalPath != null && doc != null) {
            final String effectivePath = finalPath;

            try {
                siteService.savePageIfNew(this.siteId, effectivePath, statusCode, htmlContent);

                if (statusCode == 200) {
                    siteRepository.findById(this.siteId).ifPresent(site -> {
                        pageRepository.findBySiteAndPath(site, effectivePath).ifPresent(p -> {
                            siteService.indexPageContent(p.getId());
                        });
                    });
                }
                System.out.println("DONE: " + effectivePath + " (" + statusCode + ")");

                Elements links = doc.select("a[href]");
                String currentDomain = extractDomainName(this.siteUrl);

                List<SiteIndexer> tasksToLaunch = links.stream()
                        .map(link -> link.attr("abs:href"))
                        .filter(href -> href.startsWith("http://") || href.startsWith("https://"))
                        .map(href -> href.contains("#") ? href.substring(0, href.indexOf("#")) : href)
                        .map(href -> href.contains("?") ? href.substring(0, href.indexOf("?")) : href)
                        .map(href -> href.replaceAll("/+$", ""))
                        .map(href -> href.replaceAll("(?<!https?:)/{2,}", "/"))
                        .filter(href -> href.contains(currentDomain))
                        .filter(href -> !isBinaryFile(href))
                        .filter(href -> !href.matches(".*\\.(jpg|jpeg|png|gif|webp|pdf|eps|zip|docx?|xlsx?|mp3|mp4)$"))
                        .filter(href -> siteService.addVisitedUrlFilter(href))
                        .filter(href -> {
                            String targetPath = href.replace(this.siteUrl, "");
                            if (targetPath.isEmpty()) targetPath = "/";
                            if (!targetPath.startsWith("/")) targetPath = "/" + targetPath;
                            return !siteService.isPageAlreadyIndexed(this.siteId, targetPath);
                        })
                        .map(link -> new SiteIndexer(link, this.siteId, this.siteUrl, siteService, pageRepository, siteRepository))
                        .collect(Collectors.toList());

                if (!tasksToLaunch.isEmpty()) {
                    invokeAll(tasksToLaunch);
                }
            } catch (Exception e) {
                System.err.println("CRITICAL ERROR during indexing " + url + ": " + e.getMessage());
            }
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
