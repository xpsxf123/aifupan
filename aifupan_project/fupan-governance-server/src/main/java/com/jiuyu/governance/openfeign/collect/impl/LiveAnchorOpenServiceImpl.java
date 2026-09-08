package com.jiuyu.governance.openfeign.collect.impl;

import com.jiuyu.framework.util.EmptyUtil;
import com.jiuyu.governance.business.room.pojo.constants.LivePlatformType;
import com.jiuyu.governance.openfeign.collect.LiveAnchorOpenService;
import com.jiuyu.governance.openfeign.collect.LiveAnchorServerProperties;
import com.jiuyu.governance.openfeign.collect.pojo.response.DouyinAnchorBo;
import com.jiuyu.governance.openfeign.collect.pojo.response.DouyinOpenResult;
import com.jiuyu.governance.plugins.http.LightweightLoadBalancerInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

/**
 * 抖音开放API
 *
 * @author HeHui
 * @date 2026-03-25 18:55
 */
@Service
@Slf4j
@EnableConfigurationProperties(LiveAnchorServerProperties.class)
public class LiveAnchorOpenServiceImpl implements LiveAnchorOpenService {


    private final LiveAnchorServerProperties properties;

    private final RestClient restClient;

    private final ParameterizedTypeReference<DouyinOpenResult<DouyinAnchorBo>> AnchorType = new ParameterizedTypeReference<>() {
    };

    public LiveAnchorOpenServiceImpl(LiveAnchorServerProperties properties, RestClient.Builder clientBuilder) {
        this.properties = properties;
        RestClient.Builder builder = clientBuilder.baseUrl(properties.getBaseUrl());
        if (properties.getEnableLoadBalance()) {
            builder.requestInterceptor(new LightweightLoadBalancerInterceptor(properties.getServiceInstances()));
        }
        this.restClient = builder.build();
    }

    /**
     * 搜索主播
     *
     * @param platformType 平台类型
     * @param keyword      关键字 抖音号或昵称
     * @param isUnique     是否启用精准匹配，精准匹配只限于搜抖音号 0: 否 1: 是
     *
     * @return {@link DouyinOpenResult }<{@link DouyinAnchorBo }>
     */
    @Override
    public DouyinOpenResult<DouyinAnchorBo> searchAnchor(LivePlatformType platformType, String keyword, boolean isUnique) {
        if (platformType != LivePlatformType.DOU_YIN) {
            return new DouyinOpenResult<>(500, "暂不支持该平台", null);
        }
        if (EmptyUtil.isEmpty(keyword)) {
            return new DouyinOpenResult<>(400, "参数错误", null);
        }
        // 构建请求参数
        String url = String.format("/douyin/search_user?keyword=%s&is_unique=%d",
            keyword, isUnique ? 1 : 0);
        // 发送HTTP请求
        return restClient.get()
            .uri(url)
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .retrieve()
            .onStatus(HttpStatusCode::is4xxClientError, (request, response) -> {
                log.error("抖音开放API错误, status: {}, keyword: {}", response.getStatusCode(), keyword);
                throw new RuntimeException("抖音开放API错误: " + response.getStatusCode());
            })
            .onStatus(HttpStatusCode::is5xxServerError, (request, response) -> {
                log.error("抖音开放API错误, status: {}, keyword: {}", response.getStatusCode(), keyword);
                throw new RuntimeException("抖音开放API服务器错误: " + response.getStatusCode());
            }).body(AnchorType);
    }
}
