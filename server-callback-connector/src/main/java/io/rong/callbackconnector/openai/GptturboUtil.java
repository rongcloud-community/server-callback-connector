package io.rong.callbackconnector.openai;

import com.alibaba.fastjson2.JSON;
import io.rong.callbackconnector.cache.CurrentLimiting;
import io.rong.callbackconnector.cache.GptturboMsgCache;
import io.rong.callbackconnector.config.OpenAiConfig;
import io.rong.callbackconnector.config.RcConfig;
import io.rong.callbackconnector.httpcli.HttpClient;
import io.rong.callbackconnector.model.*;
import io.rong.callbackconnector.serverapi.PersonMsgUtil;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
public class GptturboUtil {

    private static final String API_URL = "https://api.openai.com/v1/chat/completions";

    private static List<GptturboModel.Message> assembleMegs(RcRequest rcRequest, GptturboModel.Message msg){
        return GptturboMsgCache.add(rcRequest.getFromUserId(),msg);
    }

    private static final Set<Character> separatorSet = new HashSet<>(15,1);
    static {
        separatorSet.add('\n');
        separatorSet.add('；');
        separatorSet.add(';');
        separatorSet.add('！');
        separatorSet.add('!');
        separatorSet.add('？');
        separatorSet.add('?');
        separatorSet.add('。');
    }

    public static void asyncGptturbo(RcConfig rcConfig, OpenAiConfig openAiConfig, ContextModel contextModel, RcRequest rcRequest, MsgModel msgModel, Request request, int failedCount){
        if(null == request) {
            List<GptturboModel.Message> messages = assembleMegs(rcRequest, new GptturboModel.Message().setRole("user").setContent(msgModel.getContent()));
            GptturboModel gptturboModel = new GptturboModel().setModel(openAiConfig.getGptturboModel()).setMessages(messages);
            String body = JSON.toJSONString(gptturboModel);
            request = new Request.Builder()
                    .url(API_URL)
                    .header("Authorization", "Bearer " + openAiConfig.getApikey())
                    .header("Content-Type", "application/json;charset:utf-8;")
                    .post(RequestBody.create(body.getBytes(StandardCharsets.UTF_8)))
                    .build();

            log.info("log:{} OPENAI_GPTTURBO_REQ parameters: {}", contextModel.getLogId(), body);
        }
        Request finalRequest = request;
        HttpClient.call(contextModel,request,new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                log.error("log:{} OPENAI_GPTTURBO_FAIL url:{} err:{}", contextModel.getLogId(),call.request().url(),e.getMessage());
                if(failedCount < openAiConfig.getApiFailedNum()){
                    CurrentLimiting.delay(rcRequest.getFromUserId());
                    PersonMsgUtil.sendPersonMsg(contextModel,rcConfig,rcRequest,new MsgModel().setTypingContentType(true),null,0);
                    GptturboUtil.asyncGptturbo(rcConfig,openAiConfig,contextModel,rcRequest,msgModel, finalRequest,failedCount+1);
                }else {
                    PersonMsgUtil.sendPersonMsg(contextModel, rcConfig, rcRequest, new MsgModel().setContent("请求 GPTTURBO 错误"), null, 0);
                    CurrentLimiting.unlock(rcRequest.getFromUserId());
                }
            }

            @Override
            public void onResponse(Call call, Response response) {
                String body = null;
                ResponseBody responseBody = response.body();
                if(responseBody != null){
                    try {
                        body = responseBody.string();
                    } catch (IOException e) {
                        log.error("log:{} OPENAI_GPTTURBO_ERR url:{} httpcode: {} 解析body体失败", contextModel.getLogId(),call.request().url(),response.code());
                    }
                }
                if (!response.isSuccessful() || StringUtils.isBlank(body)) {
                    log.error("log:{} OPENAI_GPTTURBO_ERR url:{} httpcode:{} body:{}", contextModel.getLogId(),call.request().url(),response.code(),body);
                    PersonMsgUtil.sendPersonMsg(contextModel,rcConfig,rcRequest,new MsgModel().setContent("请求 GPT 失败"),null,0);
                    CurrentLimiting.unlock(rcRequest.getFromUserId());
                }else {
                    readResult(contextModel,rcConfig,openAiConfig,rcRequest,body);
                }

            }
        });

    }


    private static void readResult(ContextModel contextModel,RcConfig rcConfig,OpenAiConfig openAiConfig, RcRequest rcRequest,String body) {
        boolean gptResultSend = false;
        log.info("log:{} OPENAI_GPTTURBO_RESULT body:{}", contextModel.getLogId(),body);

        try {
            GptturboStreamResult gptturboStreamResult = JSON.parseObject(body, GptturboStreamResult.class);
            if(null != gptturboStreamResult && null != gptturboStreamResult.getChoices() && !gptturboStreamResult.getChoices().isEmpty()){

                for (GptturboStreamResult.Choice choice : gptturboStreamResult.getChoices()){
                    if(null != choice && null != choice.getMessage() && StringUtils.isNotBlank(choice.getMessage().getContent())){
                        if(!gptResultSend){
                            gptResultSend = true;
                        }
                        if(openAiConfig.getReqAll() || choice.getMessage().getContent().length() <= openAiConfig.getCompletionsSplitLen()){
                            GptturboMsgCache.add(rcRequest.getFromUserId(),choice.getMessage());
                        }else {
                            char[] chars = choice.getMessage().getContent().toCharArray();
                            StringBuilder stringBuilder = new StringBuilder();
                            for(int i = 0;i < chars.length;i++){
                                if(i > openAiConfig.getCompletionsSplitLen() && separatorSet.contains(chars[i])){
                                    break;
                                }
                                stringBuilder.append(chars[i]);
                            }
                            GptturboMsgCache.add(rcRequest.getFromUserId(),new GptturboModel.Message().setRole(choice.getMessage().getRole()).setContent(stringBuilder.toString()));
                        }

                        String text = StringEscapeUtils.unescapeJava(choice.getMessage().getContent());
                        PersonMsgUtil.sendPersonMsg(contextModel,rcConfig,rcRequest,new MsgModel().setContent(text),null,0);
                    }
                }
            }
        }catch (Exception e){
            log.error("log:{} OPENAI_GPTTURBO_RESULT ANALYSIS_FAIL:{}", contextModel.getLogId(),e.getMessage());
        }

        if(!gptResultSend) {
            PersonMsgUtil.sendPersonMsg(contextModel,rcConfig,rcRequest,new MsgModel().setContent("没有正确的解析 GPT 的响应"),null,0);
        }
        CurrentLimiting.unlock(rcRequest.getFromUserId());
    }


}
