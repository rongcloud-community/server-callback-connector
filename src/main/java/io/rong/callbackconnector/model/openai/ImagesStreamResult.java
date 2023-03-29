package io.rong.callbackconnector.model.openai;

import com.alibaba.fastjson2.annotation.JSONField;
import lombok.Data;

import java.util.List;

@Data
public class ImagesStreamResult {

    @Data
    public static class Image {
        private String url;
        @JSONField(name = "b64_json")
        private String b64Json;
    }

    private Long created;

    private List<Image> data;

}
