package searchengine.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "page", indexes = {
        @Index(name = "idx_path", columnList = "path")
}, uniqueConstraints = {
        @UniqueConstraint(columnNames = {"site_id", "path"})
})
public class Page {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="site_id", nullable = false)
    private Site site;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String path;

    private Integer code;
}
