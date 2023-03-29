package io.rong.callbackconnector.model.openai;

import com.alibaba.fastjson2.annotation.JSONField;
import lombok.Data;

@Data
public class ImagesModel {

    private String prompt;

    private Integer n;

    private String size;

    @JSONField(name = "response_format")
    private String responseFormat;

    private String user;

}
