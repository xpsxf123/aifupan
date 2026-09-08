package com.jiuyu.replay.ai.config;

import com.jiuyu.replay.common.utils.RRException;
import com.jiuyu.replay.generic.bo.ai.AiModelBo;
import com.volcengine.ark.runtime.service.ArkService;
import okhttp3.ConnectionPool;
import okhttp3.Dispatcher;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * @author ：lujie
 * @description：获取豆包的ArkService
 * @date ：2025/3/20 上午11:18
 */
public class ArkServiceClient {

    public static Map<String, ArkService> arkServiceMap = new ConcurrentHashMap<>();

    /**
     * 获取豆包服务
     * @param aiModel
     * @return
     */
    public static ArkService getArkService(AiModelBo aiModel) {

        if (aiModel == null){
            RRException.create("ai配置不能为空");
        }
        if (aiModel.getResourceType() != 0){
            RRException.create("ai配置不是豆包的配置");
        }

        if (arkServiceMap.containsKey(aiModel.getApiKey())){
            return arkServiceMap.get(aiModel.getApiKey());
        }

        synchronized (ArkServiceClient.class){
            if (arkServiceMap.containsKey(aiModel.getApiKey())){
                return arkServiceMap.get(aiModel.getApiKey());
            }

            Dispatcher dispatcher = new Dispatcher();
            ConnectionPool connectionPool = new ConnectionPool(5, 1, TimeUnit.SECONDS);
            ArkService service = ArkService.builder().dispatcher(dispatcher).connectionPool(connectionPool).baseUrl("https://ark.cn-beijing.volces.com/api/v3")
                    .apiKey(aiModel.getApiKey())
                    .timeout(Duration.ofSeconds(60 * 10))
                    .connectTimeout(Duration.ofSeconds(20))
                    .build();
            arkServiceMap.put(aiModel.getApiKey(), service);
            return service;
        }
    }

}
