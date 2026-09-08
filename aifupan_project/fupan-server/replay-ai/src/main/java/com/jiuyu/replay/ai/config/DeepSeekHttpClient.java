package com.jiuyu.replay.ai.config;

import com.jiuyu.replay.common.utils.RRException;
import com.jiuyu.replay.generic.bo.ai.AiModelBo;
import okhttp3.ConnectionPool;
import okhttp3.Dispatcher;
import okhttp3.OkHttpClient;
import okhttp3.Request;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class DeepSeekHttpClient {

    public static final String BASE_URL = "https://api.deepseek.com";
    public static final String CHAT_COMPLETIONS_PATH = "/v1/chat/completions";

    public static Map<String, OkHttpClient> clientMap = new ConcurrentHashMap<>();

    public static OkHttpClient getClient(AiModelBo aiModel) {
        if (aiModel == null) {
            RRException.create("ai配置不能为空");
        }
        if (aiModel.getResourceType() != 2) {
            RRException.create("ai配置不是DeepSeek的配置");
        }

        if (clientMap.containsKey(aiModel.getApiKey())) {
            return clientMap.get(aiModel.getApiKey());
        }

        synchronized (DeepSeekHttpClient.class) {
            if (clientMap.containsKey(aiModel.getApiKey())) {
                return clientMap.get(aiModel.getApiKey());
            }

            Dispatcher dispatcher = new Dispatcher();
            ConnectionPool connectionPool = new ConnectionPool(5, 1, TimeUnit.SECONDS);
            OkHttpClient client = new OkHttpClient.Builder()
                    .dispatcher(dispatcher)
                    .connectionPool(connectionPool)
                    .connectTimeout(20, TimeUnit.SECONDS)
                    .readTimeout(10, TimeUnit.MINUTES)
                    .writeTimeout(10, TimeUnit.MINUTES)
                    .callTimeout(10, TimeUnit.MINUTES)
                    .addInterceptor(chain -> {
                        Request original = chain.request();
                        Request request = original.newBuilder()
                                .header("Authorization", "Bearer " + aiModel.getApiKey())
                                .header("Content-Type", "application/json")
                                .build();
                        return chain.proceed(request);
                    })
                    .build();
            clientMap.put(aiModel.getApiKey(), client);
            return client;
        }
    }
}
