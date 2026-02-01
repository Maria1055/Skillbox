package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.IndexModel;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;

import java.util.*;

@Service
@RequiredArgsConstructor
public class PageIndexer {
    private final PageRepository pageRepository;
    private final LemmaParser lemmaParser;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void indexPageContent(Integer pageId) {
        Page page = pageRepository.findById(pageId).orElseThrow(
                () -> new RuntimeException("Page not found: " + pageId)
        );

        if (page.getContent() == null || page.getContent().isBlank()) return;

        Site site = page.getSite();
        String text = lemmaParser.clearHtmlFromTags(page.getContent());
        Map<String, Integer> lemmas = lemmaParser.getLemmaCount(text);

        if (lemmas.isEmpty()) return;

        indexRepository.deleteByPage(page);

        List<IndexModel> indexList = new ArrayList<>();

        for (Map.Entry<String, Integer> entry : lemmas.entrySet()) {
            String lemmaText = entry.getKey();

            Lemma lemma = lemmaRepository.findByLemmaAndSite(lemmaText, site)
                    .orElseGet(() -> {
                        Lemma newL = new Lemma();
                        newL.setLemma(lemmaText);
                        newL.setSite(site);
                        newL.setFrequency(0);
                        return newL;
                    });

            lemma.setFrequency(lemma.getFrequency() + 1);
            lemmaRepository.save(lemma);

            IndexModel index = new IndexModel();
            index.setPage(page);
            index.setLemma(lemma);
            index.setRank(entry.getValue().floatValue());
            indexList.add(index);
        }

        if (!indexList.isEmpty()) {
            indexRepository.saveAll(indexList);
        }
    }
}
