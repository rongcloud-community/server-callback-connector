package io.rong.callbackconnector.model.openai;

import lombok.Data;

@Data
public class ContextModel {

    private String logId;

    private String requestId;

    private boolean image;

}
