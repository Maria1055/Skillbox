package searchengine.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "lemma", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"lemma", "site_id"})
})
public class Lemma {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="id")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="site_id", referencedColumnName = "id", nullable = false)
    private Site site;

    @Column(name="lemma", nullable = false)
    private String lemma;

    @Column(name="frequency", nullable = false)
    private Integer frequency;



}
