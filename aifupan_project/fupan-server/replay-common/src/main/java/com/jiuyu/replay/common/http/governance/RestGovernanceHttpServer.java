package com.jiuyu.replay.common.http.governance;

import com.jiuyu.framework.util.EmptyUtil;
import com.jiuyu.replay.common.http.LightweightLoadBalancerInterceptor;
import com.jiuyu.replay.generic.feign.power.UserFeign;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.generic.vo.power.UserCacheVo;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;

/**
 * 爱复盘内部调用的服务实现
 *
 * @author HeHui
 * @date 2026-03-17 16:22
 */
@Service
@EnableConfigurationProperties(FupanServerProperties.class)
public class RestGovernanceHttpServer implements GovernanceHttpServer {

    private final RestClient restClient;

    private final String tokenName;

    private final UserFeign userFeign;

    private final FupanServerProperties properties;

    public RestGovernanceHttpServer(RestClient.Builder clientBuilder, FupanServerProperties properties, UserFeign userFeign) {
        this.userFeign = userFeign;
        RestClient.Builder builder = clientBuilder.baseUrl(properties.getBaseUrl());
        if (properties.getEnableLoadBalance()) {
            builder.requestInterceptor(new LightweightLoadBalancerInterceptor(properties.getServiceInstances()));
        }
        this.restClient = builder.build();
        this.tokenName = properties.getTokenName();
        this.properties = properties;
    }


    /**
     * 添加请求头
     *
     * @param spec    请求对象
     * @param headers 请求头
     *
     * @return {@link RestClient.RequestHeadersSpec }<{@link ? }>
     */
    private RestClient.RequestHeadersSpec<?> headers(RestClient.RequestHeadersSpec<?> spec, Map<String, String> headers) {
        // 添加token
        R<UserCacheVo> result = userFeign.getLocalUser();
        if (result.success() && result.getData() != null) {
            spec.header(tokenName, result.getData().getToken());
        }
        // 默认的apiKey
        spec.header(properties.getClientAppHandler(), properties.getClientAppId())
            .header(properties.getClientSecretHandler(), properties.getClientAppSecret());
        if (EmptyUtil.isEmpty(headers)) {
            return spec;
        }
        headers.forEach(spec::header);
        return spec;
    }

    /**
     * get请求
     *
     * @param url     请求地址
     * @param headers 请求头
     *
     * @return {@link RestClient.RequestHeadersSpec }<{@link ? }>
     */
    @Override
    public RestClient.RequestHeadersSpec<?> get(String url, Map<String, String> headers) {
        return this.headers(restClient.get().uri(url)
            .accept(MediaType.APPLICATION_JSON), headers);
    }

    /**
     * post请求
     *
     * @param url     请求地址
     * @param body    请求参数
     * @param headers 请求头
     *
     * @return {@link RestClient.RequestHeadersSpec }<{@link ? }>
     */
    @Override
    public RestClient.RequestHeadersSpec<?> post(String url, Object body, Map<String, String> headers) {
        return this.headers(restClient.post().uri(url).contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON).body(body), headers);
    }

    /**
     * put请求
     *
     * @param url     请求地址
     * @param body    请求参数
     * @param headers 请求头
     *
     * @return {@link RestClient.RequestHeadersSpec }<{@link ? }>
     */
    @Override
    public RestClient.RequestHeadersSpec<?> put(String url, Object body, Map<String, String> headers) {
        return this.headers(restClient.put().uri(url).contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON).body(body), headers);
    }

    /**
     * delete请求
     *
     * @param url     api地址
     * @param headers 请求头
     *
     * @return {@link RestClient.RequestHeadersSpec }<{@link ? }>
     */
    @Override
    public RestClient.RequestHeadersSpec<?> delete(String url, Map<String, String> headers) {
        return this.headers(restClient.delete().uri(url)
            .accept(MediaType.APPLICATION_JSON), headers);
    }

    /**
     * patch请求
     *
     * @param url     删除地址
     * @param body    请求参数
     * @param headers 请求头
     *
     * @return 响应数据
     */
    @Override
    public RestClient.RequestHeadersSpec<?> patch(String url, Object body, Map<String, String> headers) {
        return this.headers(restClient.patch().uri(url).contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .body(body), headers);
    }
}
