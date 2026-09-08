package com.jiuyu.replay.third.douyin;

import com.jiuyu.framework.json.JsonTemplate;
import com.jiuyu.framework.util.EmptyUtil;
import com.jiuyu.replay.generic.bo.douyin.DouyinAnchorBo;
import com.jiuyu.replay.generic.bo.douyin.DouyinOpenResult;
import com.jiuyu.replay.generic.feign.douyin.DouyinOpenFeign;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * 抖音开放API实现类
 *
 * @author HeHui
 * @date 2026-02-06 10:46
 */
@Component
@Slf4j
public class DouyinOpenFeignImpl implements DouyinOpenFeign {

    private final RestClient restClient;

    public DouyinOpenFeignImpl(@Value("${third.douyin.base-url}") String baseUrl) {
        this.restClient = RestClient.create(baseUrl);
    }


    /**
     * 搜索抖音主播
     *
     * @param keyword  关键字 抖音号或昵称
     * @param isUnique 是否启用精准匹配，精准匹配只限于搜抖音号 0: 否 1: 是
     *
     * @return 抖音主播信息
     */
    @Override
    public DouyinOpenResult<DouyinAnchorBo> searchAnchor(String keyword, boolean isUnique) {
        if (EmptyUtil.isEmpty(keyword)) {
            return new DouyinOpenResult<>(400, "参数错误", null);
        }
        try {
            // 构建请求参数
            String url = String.format("/douyin/search_user?keyword=%s&is_unique=%d",
                keyword, isUnique ? 1 : 0);
            // 发送HTTP请求
            String responseJson = restClient.get()
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
                })
                .body(String.class);

            // 解析响应
            if (responseJson != null) {
                return JsonTemplate.parser(responseJson, DouyinOpenResult.class, DouyinAnchorBo.class);
            } else {
                return new DouyinOpenResult<>(500, "抖音API响应为空", null);
            }
        } catch (Exception e) {
            log.error("调用抖音开放API搜索主播失败, keyword: {}", keyword, e);
            return new DouyinOpenResult<>(500, "系统内部错误: " + e.getMessage(), null);
        }
    }
}
