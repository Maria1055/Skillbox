package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import searchengine.dto.search.SearchResponse;
import searchengine.dto.search.SearchResultDto;
import searchengine.model.IndexModel;
import searchengine.model.Lemma;
import searchengine.model.Site;
import searchengine.model.Page;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;

import java.util.*;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {
    private final LemmaParser lemmaParser;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;
    private final PageRepository pageRepository;
    private final SiteRepository siteRepository;

    @Override
    @Transactional(readOnly = true)
    public SearchResponse search(String query, String siteUrl, int offset, int limit) {
        if (siteUrl != null && !siteUrl.endsWith("/")) {
            siteUrl += "/";
        }

        Map<String, Integer> queryLemmas = lemmaParser.getLemmaCount(query);
        List<Lemma> sortedLemmas = getSortedLemmas(queryLemmas, siteUrl);
        if (sortedLemmas.isEmpty()) return new SearchResponse(true, 0, new ArrayList<>());
        Set<Page> foundPages = findPagesByLemmas(sortedLemmas);
        if (foundPages.isEmpty()) return new SearchResponse(true, 0, new ArrayList<>());
        List<Page> distinctPages = foundPages.stream()
                .collect(Collectors.toMap(
                        p->p.getSite().getId() + lemmaParser.getTitle(p.getContent()),
                        p -> p,
                        (existing, replacement) -> existing
                ))
                .values().stream().toList();
        List<SearchResultDto> searchResults = calculateRelevance(distinctPages, sortedLemmas, queryLemmas);
        List<SearchResultDto> pagedResults = searchResults.stream()
                .sorted(Comparator.comparing(SearchResultDto::getRelevance).reversed())
                .skip(offset)
                .limit(limit)
                .toList();

        return new SearchResponse(true, searchResults.size(), pagedResults);
    }


    private List<Lemma> getSortedLemmas(Map<String, Integer> queryLemmas, String siteUrl) {
        List<Lemma> result = new ArrayList<>();
        if (siteUrl != null && !siteUrl.endsWith("/")) {
            siteUrl += "/";
        }
        Site site = siteUrl != null ? siteRepository.findByUrl(siteUrl).orElse(null) : null;
        long totalPagesCount = site != null ? pageRepository.countBySite(site) : pageRepository.count();
        double threshold = 0.95;

        for (String lemmaText : queryLemmas.keySet()) {
            List<Lemma> dbLemmas = site != null
                    ? lemmaRepository.findAllByLemmaAndSite(lemmaText, site)
                    : lemmaRepository.findAllByLemma(lemmaText);

            if (dbLemmas.isEmpty()) return new ArrayList<>();

            dbLemmas.stream()
                    .filter(l -> l.getFrequency() < totalPagesCount * threshold)
                    .min(Comparator.comparing(Lemma::getFrequency))
                    .ifPresent(result::add);
        }

        result.sort(Comparator.comparing(Lemma::getFrequency));
        return result;
    }



    private Set<Page> findPagesByLemmas(List<Lemma> lemmas) {
        List<Integer> pageIds = indexRepository.findByLemma(lemmas.get(0)).stream()
                .map(index -> index.getPage().getId())
                .distinct()
                .collect(Collectors.toList());

        for (int i = 1; i < lemmas.size(); i++) {
            pageIds = indexRepository.findByPageIdInAndLemma(pageIds, lemmas.get(i)).stream()
                    .map(index -> index.getPage().getId())
                    .distinct()
                    .collect(Collectors.toList());

            if (pageIds.isEmpty()) break;
        }

        return new HashSet<>(pageRepository.findAllById(pageIds));
    }

    private List<SearchResultDto> calculateRelevance(List<Page> pages, List<Lemma> lemmas, Map<String, Integer> queryLemmas) {
        Map<Page, Float> absRelevanceMap = new HashMap<>();
        float maxAbsRelevance = 0.0f;
        for (Page page : pages) {
            float absRelevance = 0.0f;
            for (Lemma lemma : lemmas) {
                List<IndexModel> indexes = indexRepository.findByPageAndLemma(page, lemma);
                for (IndexModel index : indexes) {
                    absRelevance += index.getRank();
                }
            }
            absRelevanceMap.put(page, absRelevance);
            if (absRelevance > maxAbsRelevance) {
                maxAbsRelevance = absRelevance;
            }
        }

        if (maxAbsRelevance == 0) return new ArrayList<>();
        final float finalMaxAbs = maxAbsRelevance;
        return absRelevanceMap.entrySet().stream()
                .map(entry -> {
                    float relRelevance = entry.getValue() / finalMaxAbs;
                    return convertToDto(entry.getKey(), relRelevance, queryLemmas);
                })
                .sorted(Comparator.comparing(SearchResultDto::getRelevance).reversed())
                .toList();
    }


    private SearchResultDto convertToDto(Page page, float relevance, Map<String, Integer> queryLemmas) {
        SearchResultDto dto = new SearchResultDto();
        dto.setSite(page.getSite().getUrl());
        dto.setSiteName(page.getSite().getName());
        dto.setUri(page.getPath());
        dto.setRelevance((double) relevance);

        String title = lemmaParser.getTitle(page.getContent());
        if (title == null || title.isBlank()) {
            title = page.getPath();
        }
        dto.setTitle(title);

        dto.setSnippet(lemmaParser.createSnippet(page.getContent(), queryLemmas));

        return dto;
    }

}
