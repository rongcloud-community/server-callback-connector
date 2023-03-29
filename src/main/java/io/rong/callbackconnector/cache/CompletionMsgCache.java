package io.rong.callbackconnector.cache;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class CompletionMsgCache {

    private static LoadingCache<String, LinkedList<String>> MSG_CACHE;

    private static int MAX_LEN = 2;

    public static void init(long minutes,int maxLen){
        MSG_CACHE = Caffeine.newBuilder()
                .expireAfterWrite(minutes, TimeUnit.MINUTES)
                .build(msgId -> new LinkedList<>());
        MAX_LEN = maxLen;
    }

    public static List<String> add(String userId,String msg){
        LinkedList<String> list = MSG_CACHE.get(userId);
        if(list.size() > MAX_LEN){
            list.removeFirst();
        }
        list.add(msg);
        MSG_CACHE.put(userId,list);
        return list;
    }

    public static void clean(String userId){
        MSG_CACHE.invalidate(userId);
    }


}
