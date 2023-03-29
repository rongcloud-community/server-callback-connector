package io.rong.callbackconnector.filter;

import io.rong.callbackconnector.context.ContextHolder;
import io.rong.callbackconnector.model.openai.ContextModel;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;

@Slf4j
@Order(90000)
@Component
@WebFilter(urlPatterns = {"/**"},filterName = "logFilter")
public class LogFilter implements Filter {


    /**
     * http请求头中的 Request-ID ，如果没有则使用 logId 拼接自己生成一个，请求融云 IM 的 serverApi 服务的时候再给带回去。方便出现问题后排查问题
     */
    public static final String HEADER_NAME = "X-Request-ID";

    /**
     * 生成 logId 的方法，便于打印同一个请求的处理日志。如果是集群部署并且统一收集日志的话，需要将其调整，防止多节点日志id冲突
     */
    private static final char[] DIGITS64 = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ-_".toCharArray();
    private static String toIDString(long l) {
        int length = 11;
        char[] buf = new char[length];
        long least = 63L;
        do {
            --length;
            buf[length] = DIGITS64[(int)(l & least)];
            l >>>= 6;
        } while(l != 0L);

        return new String(buf);
    }
    private String logId(){
        UUID u = UUID.randomUUID();
        return toIDString(u.getMostSignificantBits()) + toIDString(u.getLeastSignificantBits());
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        Filter.super.init(filterConfig);
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        String logId = logId();
        HttpServletRequest httpServletRequest = (HttpServletRequest) servletRequest;
        String requestId = httpServletRequest.getHeader(HEADER_NAME);
        if(StringUtils.isBlank(requestId)){
            requestId = "rc_"+logId;
        }
        log.info("log:{} RequestFilter  requestId: {}", logId, requestId);
        HttpServletResponse httpServletResponse = (HttpServletResponse) servletResponse;
        httpServletResponse.setHeader(HEADER_NAME,requestId);
        ContextHolder.set(new ContextModel().setLogId(logId).setRequestId(requestId));
        filterChain.doFilter(httpServletRequest,httpServletResponse);
    }

    @Override
    public void destroy() {
        Filter.super.destroy();
    }

}
