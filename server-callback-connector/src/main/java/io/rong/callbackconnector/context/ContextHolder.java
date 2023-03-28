package io.rong.callbackconnector.context;


import io.rong.callbackconnector.model.ContextModel;

public class ContextHolder {

    private static final ThreadLocal<ContextModel> contextHolder = new ThreadLocal<>();

    public static ContextModel get() {
        return contextHolder.get();
    }

    public static void set(ContextModel contextModel) {
        contextHolder.set(contextModel);
    }

    public static void del() {
        contextHolder.remove();
    }

}
