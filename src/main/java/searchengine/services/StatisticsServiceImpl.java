
package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.config.SiteConfig;
import searchengine.config.SitesList;
import searchengine.dto.statistics.DetailedStatisticsItem;
import searchengine.dto.statistics.StatisticsData;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.dto.statistics.TotalStatistics;
import searchengine.model.Site;
import searchengine.model.Status;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;


    @Service
    @RequiredArgsConstructor
    public class StatisticsServiceImpl implements StatisticsService {

        private final SiteRepository siteRepository;
        private final PageRepository pageRepository;
        private final LemmaRepository lemmaRepository;

        @Override
        public StatisticsResponse getStatistics() {
            List<Site> dbSites = siteRepository.findAll();

            TotalStatistics total = new TotalStatistics();
            total.setSites(dbSites.size());
            boolean isAnyIndexing = dbSites.stream()
                    .anyMatch(s -> s.getStatus() == Status.INDEXING);
            total.setIndexing(isAnyIndexing);

            List<DetailedStatisticsItem> detailed = new ArrayList<>();

            long totalPages = 0;
            long totalLemmas = 0;

            for (Site site : dbSites) {
                int pagesCount = (int) pageRepository.countBySite(site);
                int lemmasCount = (int) lemmaRepository.countBySite(site);

                DetailedStatisticsItem item = new DetailedStatisticsItem();
                item.setName(site.getName());
                item.setUrl(site.getUrl());
                item.setPages(pagesCount);
                item.setLemmas(lemmasCount);
                item.setStatus(site.getStatus().toString());
                item.setError(site.getLastError() == null ? "" : site.getLastError());
                item.setStatusTime(site.getStatusTime().toEpochMilli());

                totalPages += pagesCount;
                totalLemmas += lemmasCount;
                detailed.add(item);
            }
            total.setPages((int) totalPages);
            total.setLemmas((int) totalLemmas);

            StatisticsData data = new StatisticsData();
            data.setTotal(total);
            data.setDetailed(detailed);

            StatisticsResponse response = new StatisticsResponse();
            response.setStatistics(data);
            response.setResult(true);

            return response;
        }
    }

