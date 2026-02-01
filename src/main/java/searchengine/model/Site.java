package searchengine.model;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;


import java.time.Instant;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name="site")
public class Site {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="id")
    private Integer id;

    @Enumerated(EnumType.STRING)
    @Column(name="status", nullable = false, length = 20)
    private Status status;

    @CreationTimestamp
    @Column(name="status_time")
    private Instant statusTime;

    @Column(name= "last_error")
    private String lastError;

    @Column(name="url", columnDefinition = "VARCHAR(255)")
    private String url;

    @Column(name="name", columnDefinition = "VARCHAR(255)")
    private String name;

    @OneToMany(mappedBy = "site", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Page> pages;

    @OneToMany(mappedBy = "site", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Lemma> lemmas;

}
