package searchengine.repositories;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.Page;
import searchengine.model.Site;

import java.util.Optional;

@Repository
public interface PageRepository extends JpaRepository<Page, Integer> {

    boolean existsBySiteAndPath(Site site, String path);
    Optional<Page> findBySiteAndPath(Site site, String path);
    void deleteBySite(Site site);
    long countBySite(Site site);
}
