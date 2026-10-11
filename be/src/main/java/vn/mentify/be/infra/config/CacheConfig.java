package vn.mentify.be.infra.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@EnableCaching
public class CacheConfig {

    public static final String CACHE_SCHOOLS = "masterdata_schools";
    public static final String CACHE_MAJOR_GROUPS = "masterdata_major_groups";

    @Bean
    public CacheManager cacheManager() {
        ConcurrentMapCacheManager cacheManager = new ConcurrentMapCacheManager();
        cacheManager.setCacheNames(List.of(CACHE_SCHOOLS, CACHE_MAJOR_GROUPS));
        return cacheManager;
    }
}
