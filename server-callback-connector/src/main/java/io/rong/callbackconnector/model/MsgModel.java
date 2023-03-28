package io.rong.callbackconnector.model;

import com.alibaba.fastjson2.JSONObject;
import lombok.Data;

@Data
public class MsgModel {
    private String content;
    private boolean typingContentType;
    private boolean referenceMsg;
    private String localPath = "";
    private String imageUri;
    private String referMsgUserId;
    private JSONObject referMsg;
    private String objName;
    private MentionedInfo mentionedInfo;

    @Data
    public static class MentionedInfo {
        private int type = 1;
    }
}
