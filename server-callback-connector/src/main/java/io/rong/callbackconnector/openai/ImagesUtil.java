package io.rong.callbackconnector.openai;

import com.alibaba.fastjson2.JSON;
import io.rong.callbackconnector.cache.CurrentLimiting;
import io.rong.callbackconnector.config.OpenAiConfig;
import io.rong.callbackconnector.config.RcConfig;
import io.rong.callbackconnector.httpcli.HttpClient;
import io.rong.callbackconnector.model.*;
import io.rong.callbackconnector.serverapi.PersonMsgUtil;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import okhttp3.*;
import org.apache.commons.lang3.StringUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Slf4j
public class ImagesUtil {

    private static final String API_URL = "https://api.openai.com/v1/images/generations";

    private static ImagesModel assembleImages(RcRequest rcRequest, OpenAiConfig openAiConfig, MsgModel msgModel){
        return new ImagesModel()
                .setN(openAiConfig.getImagesN())
                .setPrompt(msgModel.getContent())
                .setUser(rcRequest.getFromUserId())
                .setSize(openAiConfig.getImagesSize())
                .setResponseFormat(openAiConfig.getImagesReqFormat());
    }

    public static void asyncImages(RcConfig rcConfig, OpenAiConfig openAiConfig, ContextModel contextModel, RcRequest rcRequest, MsgModel msgModel, Request request, int failedCount) {
        if(null == request) {
            ImagesModel imagesModel = assembleImages(rcRequest, openAiConfig, msgModel);
            String body = JSON.toJSONString(imagesModel);
            request = new Request.Builder()
                    .url(API_URL)
                    .header("Authorization", "Bearer " + openAiConfig.getApikey())
                    .header("Content-Type", "application/json;charset:utf-8;")
                    .post(RequestBody.create(body.getBytes(StandardCharsets.UTF_8)))
                    .build();


            log.info("log:{} OPENAI_IMAGES_REQ parameters: {}", contextModel.getLogId(), body);
        }
        Request finalRequest = request;
        HttpClient.call(contextModel, request, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                log.error("log:{} OPENAI_IMAGES_FAIL url:{} err:{}", contextModel.getLogId(), call.request().url(), e.getMessage());
                if(failedCount < openAiConfig.getApiFailedNum()){
                    CurrentLimiting.delay(rcRequest.getFromUserId());
                    PersonMsgUtil.sendPersonMsg(contextModel,rcConfig,rcRequest,new MsgModel().setTypingContentType(true),null,0);
                    ImagesUtil.asyncImages(rcConfig,openAiConfig,contextModel,rcRequest,msgModel, finalRequest,failedCount+1);
                }else {
                    PersonMsgUtil.sendPersonMsg(contextModel, rcConfig, rcRequest, new MsgModel().setContent("请求 IMAGES 错误"), null, 0);
                    CurrentLimiting.unlock(rcRequest.getFromUserId());
                }
            }

            @Override
            public void onResponse(Call call, Response response) {

                String body = null;
                ResponseBody responseBody = response.body();
                if (responseBody != null) {
                    try {
                        body = responseBody.string();
                    } catch (IOException e) {
                        log.error("log:{} OPENAI_IMAGES_ERR url:{} httpcode: {} 解析body体失败", contextModel.getLogId(), call.request().url(), response.code());
                    }
                }

                if (!response.isSuccessful() || StringUtils.isBlank(body)) {
                    log.error("log:{} OPENAI_IMAGES_ERR url:{} httpcode:{} body:{}", contextModel.getLogId(), call.request().url(), response.code(), body);
                    PersonMsgUtil.sendPersonMsg(contextModel, rcConfig, rcRequest, new MsgModel().setContent("请求 IMAGES 失败"), null, 0);
                    CurrentLimiting.unlock(rcRequest.getFromUserId());
                } else {
                    readResult(contextModel, rcConfig, rcRequest, body);
                }
            }
        });

    }

    private static void readResult(ContextModel contextModel,RcConfig rcConfig, RcRequest rcRequest,String body) {
        boolean gptResultSend = false;
        log.info("log:{} OPENAI_IMAGES_RESULT body:{}", contextModel.getLogId(),body);
        contextModel.setImage(true);
        try {
            ImagesStreamResult imagesStreamResult = JSON.parseObject(body, ImagesStreamResult.class);
            if(null != imagesStreamResult && null != imagesStreamResult.getData() && !imagesStreamResult.getData().isEmpty()){

                for (ImagesStreamResult.Image image : imagesStreamResult.getData()){
                    if(null != image && (StringUtils.isNotBlank(image.getUrl()) || StringUtils.isNotBlank(image.getB64Json()))){
                        if(!gptResultSend){
                            gptResultSend = true;
                        }
                        MsgModel msgModel = new MsgModel();
                        if(StringUtils.isNotBlank(image.getUrl())){
                            URL url = new URL(image.getUrl());
                            try (ByteArrayOutputStream baos = new ByteArrayOutputStream();){
                                Thumbnails.of(url).width(100).outputQuality(0.4).toOutputStream(baos);
                                msgModel.setContent(Base64.getEncoder().encodeToString(baos.toByteArray()));
                            }
                            msgModel.setImageUri(image.getUrl());
                            if(StringUtils.isBlank(msgModel.getContent())){
                                msgModel.setContent(image.getUrl());
                            }
                        }else {
                            msgModel.setImageUri(image.getB64Json()).setContent(image.getB64Json());
                        }
                        PersonMsgUtil.sendPersonMsg(contextModel,rcConfig,rcRequest,msgModel,null,0);
                    }
                }
            }
        }catch (Exception e){
            log.error("log:{} OPENAI_IMAGES_RESULT ANALYSIS_FAIL:{}", contextModel.getLogId(),e.getMessage());
        }

        if(!gptResultSend) {
            contextModel.setImage(false);
            PersonMsgUtil.sendPersonMsg(contextModel,rcConfig,rcRequest,new MsgModel().setContent("没有正确的解析 GPT 的响应"),null,0);
        }
        CurrentLimiting.unlock(rcRequest.getFromUserId());
    }

}
