package io.rong.callbackconnector.serverapi;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;

public class SignatureUtil {

    public static String hexSHA1(String value) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            md.update(value.getBytes(StandardCharsets.UTF_8));
            byte[] digest = md.digest();
            return Hex.encodeHexString(digest);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public static String signature(String secret,long random,long timestamp){
        if(StringUtils.isBlank(secret)){
            return "";
        }
        return SignatureUtil.hexSHA1(new StringBuilder().append(secret).append(random).append(timestamp).toString());
    }

    public static boolean check(String signature,String secret,long random,long timestamp){
        if(StringUtils.isBlank(signature) || StringUtils.isBlank(secret)){
            return false;
        }
        return signature(secret,random,timestamp).equals(signature);
    }

    private static final SecureRandom secureRandom = new SecureRandom();
    public static long random() {
        return secureRandom.nextLong();
    }

    public static long timestamp(){
        return System.currentTimeMillis();
    }
}
