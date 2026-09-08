package com.jiuyu.replay.common.http.governance;


import org.springframework.web.client.RestClient;

import java.util.Map;

/**
 * 爱复盘内部调用governance服务的实例
 *
 * @author HeHui
 * @date 2026-03-17 16:02
 */
public interface GovernanceHttpServer {

    /**
     * get请求
     *
     * @param url     请求地址
     * @param headers 请求头
     *
     * @return {@link RestClient.RequestHeadersSpec }<{@link ? }>
     */
    RestClient.RequestHeadersSpec<?> get(String url,  Map<String, String> headers);


    /**
     * post请求
     *
     * @param url     请求地址
     * @param body    请求参数
     * @param headers 请求头
     *
     * @return {@link RestClient.RequestHeadersSpec }<{@link ? }>
     */
    RestClient.RequestHeadersSpec<?> post(String url, Object body, Map<String, String> headers);


    /**
     * put请求
     *
     * @param url     请求地址
     * @param body    请求参数
     * @param headers 请求头
     *
     * @return {@link RestClient.RequestHeadersSpec }<{@link ? }>
     */
    RestClient.RequestHeadersSpec<?> put(String url, Object body, Map<String, String> headers);

    /**
     * delete请求
     *
     * @param url     api地址
     * @param headers 请求头
     *
     * @return {@link RestClient.RequestHeadersSpec }<{@link ? }>
     */
    RestClient.RequestHeadersSpec<?> delete(String url, Map<String, String> headers);

    /**
     * patch请求
     *
     * @param url   删除地址
     * @param body  请求参数
     * @param headers 请求头
     *
     * @return 响应数据
     */
    RestClient.RequestHeadersSpec<?> patch(String url, Object body, Map<String, String> headers);
}
