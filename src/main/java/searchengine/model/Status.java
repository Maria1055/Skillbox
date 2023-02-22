package searchengine.model;

import lombok.Getter;

@Getter
public enum Status {
    INDEXING,
    INDEXED,
    FAILED
}
