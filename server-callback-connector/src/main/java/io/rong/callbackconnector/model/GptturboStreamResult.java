package io.rong.callbackconnector.model;

import com.alibaba.fastjson2.annotation.JSONField;
import lombok.Data;

import java.util.List;

@Data
public class GptturboStreamResult {

    @Data
    public static class Choice {
        private Integer index;
        private GptturboModel.Message message;
        @JSONField(name = "finish_reason")
        private String finishReason;
    }

    private String id;

    private String object;

    private Long created;

    private CompletionStreamResult.Usage usage;

    private List<Choice> choices;


}
