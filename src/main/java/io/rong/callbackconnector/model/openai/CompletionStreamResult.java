package io.rong.callbackconnector.model.openai;

import com.alibaba.fastjson2.annotation.JSONField;
import lombok.Data;

import java.util.List;

@Data
public class CompletionStreamResult {

    @Data
    public static class Logprobs {
        private String tokens;
        @JSONField(name = "token_logprobs")
        private Float tokenLogprobs;
        @JSONField(name = "top_logprobs")
        private Object topLogprobs;
        @JSONField(name = "text_offset")
        private Integer textOffset;
    }

    @Data
    public static class Choice {
        private String text;
        private Integer index;
        private Logprobs logprobs;
        @JSONField(name = "finish_reason")
        private String finishReason;
    }

    @Data
    public static class Usage {
        @JSONField(name = "prompt_tokens")
        private Integer promptTokens;
        @JSONField(name = "completion_tokens")
        private Integer completionTokens;
        @JSONField(name = "total_tokens")
        private Integer totalTokens;
    }

    private String id;

    private String object;

    private Long created;

    private List<Choice> choices;

    private String model;

    private Usage usage;

}

