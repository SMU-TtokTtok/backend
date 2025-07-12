package org.project.ttokttok.global.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "club.popularity")
@Data
public class ClubPopularityConfig {
    private Weight weight = new Weight();
    private double minScore = 7.0;

    @Data
    public static class Weight {
        private double members = 0.7;
        private double favorites = 0.3;
    }
}
