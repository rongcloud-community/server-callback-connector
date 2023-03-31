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
     * 生成 logId 的方法，便于打印同一个请求的处理日志。如果是集群部署并且统一收集日志的话，需要将其调整，防止多节点日志id冲突
     * 该 logId 会在请求融云的 serverApi 时放入到请求头中，和融云的日志串联起来形成一个完整的业务链路标识，方便出现问题后排查问题
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
        log.info("RequestFilter  log:{}", logId);
        HttpServletResponse httpServletResponse = (HttpServletResponse) servletResponse;
        ContextHolder.set(new ContextModel().setLogId(logId));
        filterChain.doFilter(httpServletRequest,httpServletResponse);
    }

    @Override
    public void destroy() {
        Filter.super.destroy();
    }

}
