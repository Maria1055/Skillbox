package searchengine.dto.statistics;

import lombok.Data;
import lombok.EqualsAndHashCode;
import searchengine.dto.common.Response;

@Data
@EqualsAndHashCode(callSuper = true)
public class StatisticsResponse extends Response {
    private StatisticsData statistics;
    public StatisticsResponse (boolean result, StatisticsData statistics) {
        super(result);
        this.statistics = statistics;
    }
}