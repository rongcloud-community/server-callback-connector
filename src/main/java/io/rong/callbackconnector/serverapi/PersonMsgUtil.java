package io.rong.callbackconnector.serverapi;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import io.rong.callbackconnector.config.RcConfig;
import io.rong.callbackconnector.filter.LogFilter;
import io.rong.callbackconnector.httpcli.HttpClient;
import io.rong.callbackconnector.model.openai.ContextModel;
import io.rong.callbackconnector.model.rong.MsgModel;
import io.rong.callbackconnector.model.rong.RcRequest;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

@Slf4j
public class PersonMsgUtil {

    private static MsgModel.MentionedInfo mentionedInfo = new MsgModel.MentionedInfo();

    private static Request request(ContextModel contextModel, RcConfig rcConfig, RcRequest rcRequest, MsgModel msgModel){

        String msgType;
        String url;
        FormBody.Builder builder = new FormBody.Builder();
        if(null != msgModel.getTypingContentType() && msgModel.getTypingContentType()) {
            msgModel.setTypingContentType(null);
            msgType = "RC:TypSts";
            url = rcConfig.getApiProto()+"://"+rcConfig.getApiHost()+"/statusmessage/private/publish.json";
        }else if(null != msgModel.getReferenceMsg() && msgModel.getReferenceMsg()) {
            msgModel.setReferenceMsg(null);
            msgType = "RC:ReferenceMsg";
            url = rcConfig.getApiProto()+"://"+rcConfig.getApiHost()+"/message/private/publish.json";
            JSONObject referMsg = null;
            try {
                referMsg = JSON.parseObject(rcRequest.getContent());
            }catch (Exception e){
                log.error("log:{} CONTENT_TO_JSON_FAIL content:{} ", contextModel.getLogId(),rcRequest.getContent());
            }
            msgModel.setContent("有消息正在处理中，该消息暂不处理，请稍后重试")
                    .setReferMsgUserId(rcRequest.getFromUserId())
                    .setReferMsg(referMsg)
                    .setObjName(rcRequest.getMsgType())
                    .setMentionedInfo(mentionedInfo);
        }else {
            url = rcConfig.getApiProto()+"://"+rcConfig.getApiHost()+"/message/private/publish.json";
            builder.add("isIncludeSender","1");
            if(contextModel.isImage()){
                msgType = "RC:ImgMsg";
            }else {
                msgType = "RC:TxtMsg";
            }
        }
        String msg = JSON.toJSONString(msgModel);
        FormBody formBody = builder
            .add("fromUserId", rcConfig.getRobotUserId())
            .add("toUserId", rcRequest.getFromUserId())
            .add("objectName", msgType)
            .add("content", msg)
            .build();

        long random = SignatureUtil.random();
        long timestamp = SignatureUtil.timestamp();
        String signature = SignatureUtil.signature(rcConfig.getSecret(), random, timestamp);
        log.info("log:{} API_QUE_SEND url:{}  msg: {}", contextModel.getLogId(),url,msg);
        return new Request.Builder()
                .url(url)
                .post(formBody)
                .header("RC-App-Key", rcConfig.getAppkey())
                .header("RC-Nonce", String.valueOf(random))
                .header("RC-Timestamp", String.valueOf(timestamp))
                .header("RC-Signature", signature)
                .header("X-Request-ID", rcRequest.getMessageId())
                .build();


    }

    public static void sendPersonMsg(ContextModel contextModel, RcConfig rcConfig, RcRequest rcRequest, MsgModel msgModel, Request request,int failedCount){
        if(null == request){
            request = PersonMsgUtil.request(contextModel,rcConfig,rcRequest,msgModel);
        }
        Request finalRequest = request;
        HttpClient.call(contextModel,request, new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                log.error("log:{} API_QUE_FAIL failedCount:{} url:{} err: {}", contextModel.getLogId(),failedCount,call.request().url(),e.getMessage());
                if(failedCount < rcConfig.getApiFailedNum()){
                    PersonMsgUtil.sendPersonMsg(contextModel,rcConfig,rcRequest,msgModel, finalRequest,failedCount+1);
                }
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) {

                String body = null;
                ResponseBody responseBody = response.body();
                if(responseBody != null){
                    try {
                        body = responseBody.string();
                    } catch (IOException e) {
                        log.error("log:{} API_QUE_ERROR failedNum:{} url:{} httpcode: {} 解析body体失败", contextModel.getLogId(),failedCount,call.request().url(),response.code());
                    }
                }
                if (response.isSuccessful()){
                    log.info("log:{} API_QUE_SUCCESS failedNum:{} url:{} httpcode: {} body: {}", contextModel.getLogId(),failedCount,call.request().url(),response.code(),body);
                }else {
                    log.error("log:{} API_QUE_ERROR failedNum:{} url:{} httpcode: {} body: {}", contextModel.getLogId(),failedCount,call.request().url(),response.code(),body);
                    if(failedCount < rcConfig.getApiFailedNum()){
                        PersonMsgUtil.sendPersonMsg(contextModel,rcConfig,rcRequest,msgModel, finalRequest,failedCount+1);
                    }
                }
            }
        });
    }
}
