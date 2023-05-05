package io.rong.callbackconnector.config;

import io.rong.callbackconnector.cache.CompletionMsgCache;
import io.rong.callbackconnector.cache.CurrentLimiting;
import io.rong.callbackconnector.cache.GptturboMsgCache;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
public class OpenAiConfig {

    @Value("${openai.api_key}")
    private String apikey;

    private String type;

    @Value("${openai.completions.model}")
    private String completionsModel;

    @Value("${openai.completions.max_tokens}")
    private Integer completionsMaxTokens;

    @Value("${openai.completions.temperature}")
    private Float completionsTemperature;

    @Value("${openai.completions.split_len}")
    private Integer completionsSplitLen;

    @Value("${openai.completions.req_all}")
    private Boolean reqAll;


    @Value("${openai.gptturbo.model}")
    private String gptturboModel;

    @Value("${openai.gptturbo.system:#{null}}")
    private String gptturboSystem;

    @Value("${openai.images.n}")
    private Integer imagesN;

    @Value("${openai.images.size}")
    private String imagesSize;

    @Value("${openai.images.response_format}")
    private String imagesReqFormat;

    @Autowired
    private CacheConfig cacheConfig;

    @Value("${openai.api.failed_num}")
    private Integer apiFailedNum;

    @Value("${openai.type}")
    public void initConfig(String type){
        this.type = type;
        CurrentLimiting.init(cacheConfig.getLockSeconds());
        if("completions".equals(type)){
            CompletionMsgCache.init(cacheConfig.getContextMinutes(),cacheConfig.getCompletionsLen());
        }else {
            GptturboMsgCache.init(cacheConfig.getContextMinutes(),cacheConfig.getGptturboLen(),gptturboSystem);
        }
    }

}
