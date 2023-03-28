package io.rong.callbackconnector.openai;

import com.alibaba.fastjson2.JSON;
import io.rong.callbackconnector.cache.CompletionMsgCache;
import io.rong.callbackconnector.cache.CurrentLimiting;
import io.rong.callbackconnector.config.OpenAiConfig;
import io.rong.callbackconnector.config.RcConfig;
import io.rong.callbackconnector.httpcli.HttpClient;
import io.rong.callbackconnector.model.*;
import io.rong.callbackconnector.serverapi.PersonMsgUtil;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import okio.BufferedSource;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
public class CompletionUtil {

    private static final String API_URL = "https://api.openai.com/v1/completions";

    private static String assemblePrompt(RcRequest rcRequest, MsgModel msgModel){
        StringBuilder stringBuilder = new StringBuilder();
        List<String> list = CompletionMsgCache.add(rcRequest.getFromUserId(), msgModel.getContent());
        for (String text : list){
            stringBuilder.append(text).append("\n\n");
        }

        return stringBuilder.toString();
    }

    private static final Set<String> separatorSet = new HashSet<>(15,1);
    static {
        separatorSet.add("\n");
        separatorSet.add("；");
        separatorSet.add(";");
        separatorSet.add("！");
        separatorSet.add("!");
        separatorSet.add("？");
        separatorSet.add("?");
        separatorSet.add("……");
        separatorSet.add("。");
    }

    public static void asyncCompletion(RcConfig rcConfig, OpenAiConfig openAiConfig, ContextModel contextModel, RcRequest rcRequest, MsgModel msgModel, Request request, int failedCount){

        if(null == request){
            CompletionModel completionModel = new CompletionModel()
                    .setModel(openAiConfig.getCompletionsModel())
                    .setPrompt(assemblePrompt(rcRequest,msgModel))
                    .setMaxTokens(openAiConfig.getCompletionsMaxTokens())
                    .setTemperature(openAiConfig.getCompletionsTemperature())
                    .setUser(rcRequest.getFromUserId())
                    ;
            String body = JSON.toJSONString(completionModel);
            request = new Request.Builder()
                    .url(API_URL)
                    .header("Authorization", "Bearer " + openAiConfig.getApikey())
                    .header("Content-Type", "application/json;charset:utf-8;")
                    .post(RequestBody.create(body.getBytes(StandardCharsets.UTF_8)))
                    .build();
            log.info("log:{} OPENAI_COMPLETION_REQ parameters: {}", contextModel.getLogId(),body);
        }

        Request finalRequest = request;
        HttpClient.call(contextModel,request,new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                log.error("log:{} OPENAI_COMPLETION_FAIL url:{} err:{}", contextModel.getLogId(),call.request().url(),e.getMessage());
                if(failedCount < openAiConfig.getApiFailedNum()){
                    CurrentLimiting.delay(rcRequest.getFromUserId());
                    PersonMsgUtil.sendPersonMsg(contextModel,rcConfig,rcRequest,new MsgModel().setTypingContentType(true),null,0);
                    CompletionUtil.asyncCompletion(rcConfig,openAiConfig,contextModel,rcRequest,msgModel, finalRequest,failedCount+1);
                }else {
                    PersonMsgUtil.sendPersonMsg(contextModel,rcConfig,rcRequest,new MsgModel().setContent("请求 GPT 错误"),null,0);
                    CurrentLimiting.unlock(rcRequest.getFromUserId());
                }

            }

            @Override
            public void onResponse(Call call, Response response) {

                if (!response.isSuccessful()) {
                    String body = null;
                    ResponseBody responseBody = response.body();
                    if(responseBody != null){
                        try {
                            body = responseBody.string();
                        } catch (IOException e) {
                            log.error("log:{} OPENAI_COMPLETION_ERR url:{} httpcode: {} 解析body体失败", contextModel.getLogId(),call.request().url(),response.code());
                        }
                    }
                    log.error("log:{} OPENAI_COMPLETION_ERR url:{} httpcode:{} body:{}", contextModel.getLogId(),call.request().url(),response.code(),body);
                    PersonMsgUtil.sendPersonMsg(contextModel,rcConfig,rcRequest,new MsgModel().setContent("请求 GPT 失败"),null,0);
                    CurrentLimiting.unlock(rcRequest.getFromUserId());
                }else {
                    readResult(contextModel,rcConfig,openAiConfig,rcRequest,response);
                }

            }
        });

    }

    private static void readResult(ContextModel contextModel,RcConfig rcConfig,OpenAiConfig openAiConfig, RcRequest rcRequest,Response response) {
        StringBuilder stringBuilder = new StringBuilder();
        boolean gptResultSend = false;
        try (BufferedSource source = response.body().source()) {
            String line;
            while ((line = source.readUtf8Line()) != null) {
                if(StringUtils.isBlank(line)){
                    continue;
                }
                log.info("log:{} OPENAI_COMPLETION_RESULT line:{}", contextModel.getLogId(),line);
                if(line.startsWith("data: ")){
                    line = line.substring(6);
                }
                if("[DONE]".equals(line)){
                    break;
                }

                try {
                    CompletionStreamResult completionStreamResult = JSON.parseObject(line, CompletionStreamResult.class);
                    if(null != completionStreamResult && null != completionStreamResult.getChoices() && !completionStreamResult.getChoices().isEmpty()){

                        for (CompletionStreamResult.Choice choice : completionStreamResult.getChoices()){
                            if(null != choice && StringUtils.isNotBlank(choice.getText())){
                                String text = StringEscapeUtils.unescapeJava(choice.getText());
                                int length = stringBuilder.length();
                                if(separatorSet.contains(text) && length > openAiConfig.getCompletionsSplitLen()){
                                    if(!"\n".equals(text)){
                                        stringBuilder.append(text);
                                    }
                                    String msg = stringBuilder.toString();
                                    if(!gptResultSend || openAiConfig.getReqAll()){
                                        gptResultSend = true;
                                        CompletionMsgCache.add(rcRequest.getFromUserId(),msg);
                                    }

                                    PersonMsgUtil.sendPersonMsg(contextModel,rcConfig,rcRequest,new MsgModel().setContent(msg),null,0);
                                    stringBuilder.setLength(0);
                                }else {
                                    stringBuilder.append(text);
                                }
                            }
                        }
                    }
                }catch (Exception e){
                    log.error("log:{} OPENAI_COMPLETION_RESULT ANALYSIS_FAIL:{}", contextModel.getLogId(),e.getMessage());
                }

                CurrentLimiting.delay(rcRequest.getFromUserId());
            }
        }catch (Exception e){
            log.error("log:{} OPENAI_COMPLETION_RESULT READ_FAIL:{}", contextModel.getLogId(),e.getMessage());
            CurrentLimiting.unlock(rcRequest.getFromUserId());
        }
        String msg = stringBuilder.toString();
        if(StringUtils.isNotBlank(msg)){
            if(!gptResultSend || openAiConfig.getReqAll()){
                CompletionMsgCache.add(rcRequest.getFromUserId(),msg);
            }
            PersonMsgUtil.sendPersonMsg(contextModel,rcConfig,rcRequest,new MsgModel().setContent(msg),null,0);
        }else if(!gptResultSend) {
            PersonMsgUtil.sendPersonMsg(contextModel,rcConfig,rcRequest,new MsgModel().setContent("没有正确的解析 GPT 的响应"),null,0);
        }
        CurrentLimiting.unlock(rcRequest.getFromUserId());
    }

}
