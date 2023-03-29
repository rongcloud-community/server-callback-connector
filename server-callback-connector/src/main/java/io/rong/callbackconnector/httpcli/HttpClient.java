package io.rong.callbackconnector.httpcli;

import io.rong.callbackconnector.model.openai.ContextModel;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import okhttp3.internal.Util;
import okhttp3.internal.connection.RealCall;

import java.io.IOException;
import java.io.InputStream;
import java.net.ContentHandler;
import java.net.ContentHandlerFactory;
import java.net.URLConnection;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author wxl
 * @version 1.0
 * @date 2021/8/30 2:10 下午
 */
@Slf4j
public class HttpClient {

    private static final ThreadPoolExecutor poolExecutor = new ThreadPoolExecutor(
        16,
        Integer.MAX_VALUE,
        60,
        TimeUnit.SECONDS,
        new SynchronousQueue<>(),
        Util.threadFactory("http", false)
    );
    private static final OkHttpClient httpClient;
    static {
        Dispatcher dispatcher = new Dispatcher(poolExecutor);
        dispatcher.setMaxRequests(8000);
        dispatcher.setMaxRequestsPerHost(5000);
        httpClient = new OkHttpClient.Builder()
            .dispatcher(dispatcher)
            .connectionPool(new ConnectionPool(20000,5,TimeUnit.MINUTES))
            .connectTimeout(3000, TimeUnit.MILLISECONDS)
            .writeTimeout(18, TimeUnit.SECONDS)
            .readTimeout(110, TimeUnit.SECONDS)
            .build();
        URLConnection.setContentHandlerFactory(new CustomContentHandlerFactory(httpClient));
    }

    public static void call(ContextModel contextModel, Request request, Callback callback){
        try {
            httpClient.newCall(request).enqueue(callback);
        } catch (Throwable e) {
            log.error("log:{} HTTP_REQ_FAIL url:{}", contextModel.getLogId(),request.url());
            callback.onFailure(new RealCall(httpClient,request,false),new IOException("发起http请求失败"));
        }
    }



    // 使用OkHttp实现自定义URLConnection
    public static class CustomURLConnection extends URLConnection {
        private OkHttpClient httpClient;
        protected CustomURLConnection(OkHttpClient httpClient) {
            super(null);
            this.httpClient = httpClient;
        }
        @Override
        public void connect() {
            // 不需要处理，因为我们将使用OkHttp来执行请求
        }
        @Override
        public InputStream getInputStream() throws IOException {
            // 使用OkHttp执行请求并获得作为输入流的响应
            Request request = new Request.Builder().url(getURL()).build();
            Response response = httpClient.newCall(request).execute();
            return response.body().byteStream();
        }
    }

    public static class CustomContentHandlerFactory implements ContentHandlerFactory {
        private OkHttpClient httpClient;
        public CustomContentHandlerFactory(OkHttpClient httpClient) {
            this.httpClient = httpClient;
        }
        @Override
        public ContentHandler createContentHandler(String mimeType) {
            return new ContentHandler() {
                @Override
                public Object getContent(URLConnection urlc) {
                    return new CustomURLConnection(httpClient);
                }
            };
        }
    }

}
