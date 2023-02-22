package searchengine.repositories;

import jakarta.persistence.QueryHint;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import searchengine.model.Lemma;
import searchengine.model.Site;

import java.util.List;
import java.util.Optional;

@Repository
public interface LemmaRepository extends JpaRepository<Lemma, Integer> {
    @QueryHints(@QueryHint(name = "jakarta.persistence.cache.retrieveMode", value = "BYPASS"))
    Optional<Lemma> findByLemmaAndSite(String lemma, Site site);
    int countBySite(Site site);
    void deleteAllBySite(Site site);
    @Modifying
    @Transactional
    @Query(value = "INSERT INTO lemma (lemma, site_id, frequency) VALUES (:lemma, :siteId, 1) " +
            "ON DUPLICATE KEY UPDATE frequency = frequency + 1", nativeQuery = true)
    void upsertLemma(@Param("lemma") String lemma, @Param("siteId") int siteId);
    @Modifying
    @Transactional
    @Query(value = "UPDATE lemma SET frequency = frequency + 1 WHERE lemma = :lemma AND site_id = :siteId", nativeQuery = true)
    int incrementFrequency(@Param("lemma") String lemma, @Param("siteId") int siteId);
    List<Lemma> findAllByLemma(String lemma);
    List<Lemma> findAllByLemmaAndSite(String lemma, Site site);

}
