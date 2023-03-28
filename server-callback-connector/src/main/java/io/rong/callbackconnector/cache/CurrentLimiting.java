package io.rong.callbackconnector.cache;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class CurrentLimiting {

    /**
     * 记录用户id是否在处理中，缓存有效时间120秒，如果openai的流式响应耗时过久，在处理过程中给缓存延长有效期
     */
    private static LoadingCache<String, AtomicReference<Boolean>> CURRENT_QUE_CACHE;

    public static void init(long seconds){
        CURRENT_QUE_CACHE = Caffeine.newBuilder()
                .expireAfterWrite(seconds, TimeUnit.SECONDS)
                .build(msgId -> new AtomicReference<>(false))
        ;
    }

    public static boolean lock(String userId){
        // TODO: 2023/3/1 限频在这里处理
        return CURRENT_QUE_CACHE.get(userId).compareAndSet(false,true);
    }

    public static void delay(String userId){
        CURRENT_QUE_CACHE.put(userId,new AtomicReference<>(true));
    }

    public static void unlock(String userId){
        CURRENT_QUE_CACHE.invalidate(userId);
    }
}
