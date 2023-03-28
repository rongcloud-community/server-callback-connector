package io.rong.callbackconnector.model;

import lombok.Data;

@Data
public class RcRequest {

    private String appKey;
    private String fromUserId;
    private String targetId;
    private String msgType;
    private String content;
    private String pushContent;
    private Boolean disablePush;
    private String pushExt;
    private String expansion;
    private String extraContent;
    private String channelType;
    private String msgTimeStamp;
    private String messageId;
    private String originalMsgUID;
    private String os;
    private String busChannel;

    private Long timestamp;
    private Long nonce;
    private String signature;

}
