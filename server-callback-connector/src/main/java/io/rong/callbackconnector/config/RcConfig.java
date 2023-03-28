package io.rong.callbackconnector.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Data
@Configuration
public class RcConfig {

    @Value("${rc.appkey}")
    private String appkey;

    @Value("${rc.secret}")
    private String secret;

    @Value("${rc.api.host}")
    private String apiHost;

    @Value("${rc.api.proto}")
    private String apiProto;

    @Value("${rc.api.failed_num}")
    private Integer apiFailedNum;

    @Value("${rc.robot.userid}")
    private String robotUserId;

    @Value("#{'${rc.msg.types}'.split(',')}")
    private List<String> msgTypes;

    @Value("${rc.msg.min_len}")
    private Integer minLength;


    @Value("#{'${rc.msg.channels}'.split(',')}")
    private List<String> msgChannels;

}
