package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import searchengine.model.IndexModel;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.model.Site;

import java.util.Collection;
import java.util.List;


@Repository
public interface IndexRepository extends JpaRepository<IndexModel, Integer> {

    List<IndexModel> findByPage(Page page);
    void deleteByPage(Page page);
    void deleteAllByPage_Site(Site site);
    @Query("SELECT i FROM IndexModel i WHERE i.page.id IN :ids AND i.lemma = :lemma")
    List<IndexModel> findByPageIdInAndLemma(@Param("ids") Collection<Integer> ids, @Param("lemma") Lemma lemma);
    List<IndexModel> findByLemma(Lemma lemma);
    List<IndexModel> findByPageAndLemma(Page page, Lemma lemma);

}
