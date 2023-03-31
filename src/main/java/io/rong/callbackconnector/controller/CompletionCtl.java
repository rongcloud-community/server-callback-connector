package io.rong.callbackconnector.controller;

import com.alibaba.fastjson2.JSON;
import io.rong.callbackconnector.cache.CompletionMsgCache;
import io.rong.callbackconnector.cache.CurrentLimiting;
import io.rong.callbackconnector.cache.GptturboMsgCache;
import io.rong.callbackconnector.config.OpenAiConfig;
import io.rong.callbackconnector.config.RcConfig;
import io.rong.callbackconnector.context.ContextHolder;
import io.rong.callbackconnector.model.openai.ContextModel;
import io.rong.callbackconnector.model.rong.MsgModel;
import io.rong.callbackconnector.model.rong.RcRequest;
import io.rong.callbackconnector.model.rong.RcResult;
import io.rong.callbackconnector.openai.CompletionUtil;
import io.rong.callbackconnector.openai.GptturboUtil;
import io.rong.callbackconnector.openai.ImagesUtil;
import io.rong.callbackconnector.serverapi.PersonMsgUtil;
import io.rong.callbackconnector.serverapi.SignatureUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("completion")
public class CompletionCtl {

    @Autowired
    private RcConfig rcConfig;

    @Autowired
    private OpenAiConfig openAiConfig;

    public RcResult checkRequest(ContextModel contextModel,RcRequest rcRequest){
        log.info("log:{} REQUEST POST /completion/send  parameters: {}", contextModel.getLogId(),JSON.toJSONString(rcRequest));
        if(StringUtils.isBlank(rcRequest.getSignature()) || null == rcRequest.getNonce() || null == rcRequest.getTimestamp()){
            log.info("log:{} msg: {}", contextModel.getLogId(),"签名参数不齐全");
            return RcResult.init("缺少部分签名参数，请检查");
        }

        if(StringUtils.isBlank(rcRequest.getMsgType()) || !rcConfig.getMsgTypes().contains(rcRequest.getMsgType())){
            log.info("log:{} 不支持的消息类型: {}", contextModel.getLogId(),rcRequest.getMsgType());
            return RcResult.init("消息类型不支持，请检查");
        }

        if(StringUtils.isBlank(rcRequest.getTargetId()) || !rcConfig.getRobotUserId().equals(rcRequest.getTargetId())){
            log.info("log:{} 聊天对象不是机器人: {}", contextModel.getLogId(),rcRequest.getTargetId());
            return RcResult.init("聊天对象不是机器人，请检查");
        }

        if(StringUtils.isBlank(rcRequest.getChannelType()) || !rcConfig.getMsgChannels().contains(rcRequest.getChannelType())){
            log.info("log:{} 会话类型不支持: {}", contextModel.getLogId(),rcRequest.getChannelType());
            return RcResult.init("会话类型不支持，请检查");
        }
        return null;
    }

    public MsgModel unpackMsgModel(ContextModel contextModel,RcRequest rcRequest){
        MsgModel msgModel;
        try {
            msgModel = JSON.parseObject(rcRequest.getContent(), MsgModel.class);
        }catch (Exception e){
            log.info("log:{} 没有获取到正确的消息内容，错误信息: {}", contextModel.getLogId(),e.getMessage());
            msgModel = null;
        }
        return msgModel;
    }

    public RcResult checkMsgModel(ContextModel contextModel,RcRequest rcRequest,MsgModel msgModel){
        if(null == msgModel || StringUtils.isBlank(msgModel.getContent())){
            log.info("log:{} 没有获取到正确的消息内容: {}", contextModel.getLogId(),rcRequest.getContent());
            return RcResult.init("没有获取到正确的消息内容，请检查");
        }

        msgModel.setContent(msgModel.getContent().trim());

        if(msgModel.getContent().length() < rcConfig.getMinLength()){
            log.info("log:{} 消息长度短于允许的最小长度: {} < {}", contextModel.getLogId(),msgModel.getContent().length(),rcConfig.getMinLength());
            return RcResult.init(0,"消息长度过短，GPT 无法识别");
        }

        if(!SignatureUtil.check(rcRequest.getSignature(),rcConfig.getSecret(),rcRequest.getNonce(),rcRequest.getTimestamp())){
            log.info("log:{} msg: {}", contextModel.getLogId(),"请求签名错误");
            return RcResult.init(0,"请求签名错误");
        }

        if(!CurrentLimiting.lock(rcRequest.getFromUserId())){
            log.info("log:{} 用户: {} 有请求正在处理中", contextModel.getLogId(),rcRequest.getFromUserId());
            PersonMsgUtil.sendPersonMsg(contextModel,rcConfig,rcRequest,new MsgModel().setReferenceMsg(true),null,0);
            return RcResult.success();
        }
        return null;
    }

    public RcResult changeSubject(ContextModel contextModel,RcRequest rcRequest,MsgModel msgModel){
        if("换个话题".equals(msgModel.getContent())){
            if("completions".equals(openAiConfig.getType())){
                CompletionMsgCache.clean(rcRequest.getFromUserId());
            }else {
                GptturboMsgCache.clean(rcRequest.getFromUserId());
            }
            CurrentLimiting.unlock(rcRequest.getFromUserId());
            PersonMsgUtil.sendPersonMsg(contextModel,rcConfig,rcRequest,new MsgModel().setContent("请开始新的话题……"),null,0);
            return RcResult.success();
        }
        return null;
    }

    public RcResult sendToRobot(ContextModel contextModel,RcRequest rcRequest,MsgModel msgModel){
        PersonMsgUtil.sendPersonMsg(contextModel,rcConfig,rcRequest,new MsgModel().setTypingContentType(true),null,0);
        if(msgModel.getContent().startsWith("/image ")){
            ImagesUtil.asyncImages(rcConfig,openAiConfig,contextModel,rcRequest,msgModel.setContent(msgModel.getContent().substring(7)),null,0);
        }else if(msgModel.getContent().startsWith("/画 ")){
            ImagesUtil.asyncImages(rcConfig,openAiConfig,contextModel,rcRequest,msgModel.setContent(msgModel.getContent().substring(3)),null,0);
        } else if ("/画".equals(msgModel.getContent()) || "/image".equals(msgModel.getContent())) {
            CurrentLimiting.unlock(rcRequest.getFromUserId());
            PersonMsgUtil.sendPersonMsg(contextModel,rcConfig,rcRequest,new MsgModel().setContent("请输入生成图片的内容……"),null,0);
        } else {
            if("completions".equals(openAiConfig.getType())){
                CompletionUtil.asyncCompletion(rcConfig,openAiConfig,contextModel,rcRequest,msgModel,null,0);
            }else {
                GptturboUtil.asyncGptturbo(rcConfig,openAiConfig,contextModel,rcRequest,msgModel,null,0);
            }
        }

        return RcResult.success();
    }

    @PostMapping("send")
    public RcResult send(RcRequest rcRequest) {
        ContextModel contextModel = ContextHolder.get();
        RcResult rcResult = checkRequest(contextModel, rcRequest);
        if(null != rcResult){
            return rcResult;
        }

        MsgModel msgModel = unpackMsgModel(contextModel,rcRequest);

        rcResult = checkMsgModel(contextModel, rcRequest,msgModel);
        if(null != rcResult){
            return rcResult;
        }

        rcResult = changeSubject(contextModel, rcRequest,msgModel);
        if(null != rcResult){
            return rcResult;
        }

        return sendToRobot(contextModel, rcRequest,msgModel);

    }

}
