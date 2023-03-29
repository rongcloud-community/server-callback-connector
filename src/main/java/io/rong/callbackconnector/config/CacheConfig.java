package io.rong.callbackconnector.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
public class CacheConfig {

    @Value("${cache.context.minutes}")
    private Integer contextMinutes;

    @Value("${cache.lock_seconds}")
    private Integer lockSeconds;

    @Value("${cache.completions.len}")
    private Integer completionsLen;

    @Value("${cache.gptturbo.len}")
    private Integer gptturboLen;


}
