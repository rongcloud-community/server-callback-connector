package io.rong.callbackconnector.model;

import lombok.Data;

import java.util.List;

@Data
public class GptturboModel {

    @Data
    public static class Message {
        private String role;
        private String content;
    }


    private String model;

    private List<Message> messages;


}
