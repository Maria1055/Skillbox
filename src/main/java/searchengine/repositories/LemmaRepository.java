package searchengine.repositories;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.Lemma;
import searchengine.model.Site;

import java.util.List;
import java.util.Optional;

@Repository
public interface LemmaRepository extends JpaRepository<Lemma, Integer> {
    Optional<Lemma> findByLemmaAndSite(String lemma, Site site);
    int countBySite(Site site);
    void deleteBySite(Site site);
    List<Lemma> findAllByLemma(String lemma);
    List<Lemma> findAllByLemmaAndSite(String lemma, Site site);
}
