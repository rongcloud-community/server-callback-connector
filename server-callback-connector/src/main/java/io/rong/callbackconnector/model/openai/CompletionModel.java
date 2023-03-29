package io.rong.callbackconnector.model.openai;

import com.alibaba.fastjson2.annotation.JSONField;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class CompletionModel {

    private String model;

    private String prompt;

    private String suffix;

    @JSONField(name = "max_tokens")
    private Integer maxTokens;

    private Float temperature = 0.5F;

    @JSONField(name = "top_p")
    private Float topP;

    private Integer n;

    private boolean stream = true;

    private Integer logprobs;

    private boolean echo = false;

    private List<String> stop;

    @JSONField(name = "presence_penalty")
    private Float presencePenalty;

    @JSONField(name = "frequency_penalty")
    private Float frequencyPenalty;

    @JSONField(name = "best_of")
    private Integer bestOf;

    @JSONField(name = "logit_bias")
    private Map<String,Object> logitBias;

    private String user;

}
