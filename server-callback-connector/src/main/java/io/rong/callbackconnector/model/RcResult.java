package io.rong.callbackconnector.model;

import lombok.Data;

@Data
public class RcResult {

    private Integer pass = 1;

    private String extra;

    private final static RcResult successResult = init();

    private RcResult(){}

    public static RcResult init(){
        return new RcResult();
    }

    public static RcResult success(){
        return successResult;
    }

    public static RcResult init(Integer pass){
        return new RcResult().setPass(pass);
    }

    public static RcResult init(String extra){
        return new RcResult().setExtra(extra);
    }

    public static RcResult init(Integer pass,String extra){
        return new RcResult().setPass(pass).setExtra(extra);
    }

}
