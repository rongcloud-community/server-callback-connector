package io.rong.callbackconnector.cache;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import io.rong.callbackconnector.model.openai.GptturboModel;
import org.apache.commons.lang3.StringUtils;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class GptturboMsgCache {

    private static LoadingCache<String, LinkedList<GptturboModel.Message>> MSG_CACHE;

    private static boolean INCLUSIVE_SYSTEM = false;


    private static int MAX_LEN = 2;
    public static void init(long minutes,int maxLen,String system){
        INCLUSIVE_SYSTEM = StringUtils.isNotBlank(system);
        MSG_CACHE = Caffeine.newBuilder()
                .expireAfterWrite(minutes, TimeUnit.MINUTES)
                .build(msgId -> {
                    LinkedList<GptturboModel.Message> linkedList = new LinkedList<>();
                    if(INCLUSIVE_SYSTEM){
                        linkedList.add(new GptturboModel.Message().setRole("system").setContent(system));
                    }
                    return linkedList;
                });
        if(maxLen > 0){
            MAX_LEN = maxLen;
        }
    }

    public static void clean(String userId){
        MSG_CACHE.invalidate(userId);
    }

    public static List<GptturboModel.Message> add(String userId, GptturboModel.Message msg){
        LinkedList<GptturboModel.Message> list = MSG_CACHE.get(userId);
        if(list.size() > MAX_LEN && MAX_LEN > 1 && INCLUSIVE_SYSTEM){
            list.remove(1);
        }else if(list.size() > MAX_LEN){
            list.remove(0);
        }
        list.add(msg);
        MSG_CACHE.put(userId,list);
        return list;
    }
}
