package searchengine.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.List;


@Entity
@Getter
@Setter
@Table(name = "page", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"site_id", "path"})
})
public class Page {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="id")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="site_id", referencedColumnName = "id", nullable = false)
    private Site site;

    @OneToMany(mappedBy = "page", fetch = FetchType.LAZY)
    private List<IndexModel> indexes;

    @Column(columnDefinition = "VARCHAR(512)", nullable = false)
    private String path;

    @Column(name="code")
    private Integer code;

    @Column(name="content", columnDefinition = "MEDIUMTEXT")
    private String content;

}
