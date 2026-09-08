package com.jiuyu.replay.third.chanmama;


import cn.hutool.core.util.IdUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import com.jiuyu.framework.concurrent.Retry;
import com.jiuyu.replay.common.annotation.CustomRedissonLock;
import com.jiuyu.replay.generic.dto.third.ChanmamaRevisionDto;
import com.jiuyu.replay.generic.dto.third.SendSimilarAnchorDto;
import com.jiuyu.replay.generic.dto.third.ThirdSalesRankingResult;
import com.jiuyu.replay.generic.enums.third.ChanmamaResponseCodeEnum;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.generic.vo.common.code.StatusCode;
import com.jiuyu.replay.third.bo.chanmama.ChanmamaCache;
import com.jiuyu.replay.third.bo.chanmama.ChanmamaQueryBo;
import com.jiuyu.replay.third.constant.ChanmamaProperties;
import com.jiuyu.replay.third.entity.ChanmamaAccountEntity;
import com.jiuyu.replay.third.repository.service.ChanmamaAccountService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.security.sasl.AuthenticationException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;

@Slf4j
@Component
public class ThirdDataUtils {

    private final ChanmamaProperties chanmamaProperties;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ChanmamaAccountService chanmamaAccountService;

    private final boolean isRelease;


    private final TypeReference<R<ThirdSalesRankingResult>> ThirdSalesRankingResultClass = new TypeReference<R<ThirdSalesRankingResult>>() {};

    // 创建 HttpClient 实例
    private static final HttpClient client = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(30))
            .build();

    public ThirdDataUtils(ChanmamaProperties chanmamaProperties, RedisTemplate<String, Object> redisTemplate, ChanmamaAccountService chanmamaAccountService, Environment environment) {
        this.chanmamaProperties = chanmamaProperties;
        this.redisTemplate = redisTemplate;
        this.chanmamaAccountService = chanmamaAccountService;
        List<String> list = Arrays.asList(environment.getActiveProfiles());
        this.isRelease = list.contains("prod") || list.contains("yz");
    }

    /**
     * 登录第三方数据平台
     * @param username 第三方数据平台账号
     * @param password 第三方数据平台密码
     * @return
     */
    public String login(String username, String password) {

        Map<String, Object> param = new HashMap<>();
        param.put("username", username);
        param.put("password", password);

        // 构建 POST 请求
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(chanmamaProperties.getRequestBaseUrl() + "/api/login"))
                .header("Content-Type", "application/json") // 设置请求体类型
                .POST(HttpRequest.BodyPublishers.ofString(JSON.toJSONString(param))) // 发送 JSON 请求体
                .timeout(Duration.ofSeconds(60)) // 设置请求超时时间
                .build();

        // 发送请求并获取响应
        try {
            // 解析响应体为字符串
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if(response.statusCode() == 200) {
                String resultStr = response.body();
                R r = JSON.parseObject(resultStr, R.class);
                if(r.getCode() == 0) {
                    return (String) r.getData();
                }

            }

            log.info("登录第三方数据平台请求失败：{}，{}，{}", username, password, response.body());

        } catch (Exception e) {
            log.info("登录第三方数据平台发生异常，{}，{}", username, password);
            e.printStackTrace();
        }

        return null;
    }

    /**
     * 刷新第三方数据平台token
     * @param token 第三方数据平台token
     */
    public String refreshToken(String token) {

        // 构建 GET 请求
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(chanmamaProperties.getRequestBaseUrl() + "/api/refreshToken"))
                .header("triumphtoken", token) // 设置 Token 请求头
                .GET()
                .timeout(Duration.ofSeconds(60)) // 设置请求超时时间
                .build();

        // 发送请求并获取响应
        try {
            // 解析响应体为字符串
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if(response.statusCode() == 200) {
                String resultStr = response.body();
                R r = JSON.parseObject(resultStr, R.class);
                if(r.getCode() == 0) {
                    return (String) r.getData();
                }

            }

            log.info("刷新第三方数据平台token请求失败：{}====={}", token, response.body());

        } catch (Exception e) {
            log.info("刷新第三方数据平台token出现异常，{}", token);
            e.printStackTrace();
        }

        return null;
    }

    /**
     * 查询第三方数据平台数据
     * @param chanmamaQueryBo 查询参数
     * @return
     */
    public R<String> queryData(ChanmamaQueryBo chanmamaQueryBo) {

        int retryNum = 3;
        while (retryNum > 0) {

            // 获取token
            String token = getToken();

            // 构建 POST 请求
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(chanmamaProperties.getRequestBaseUrl() + "/api/sendMessage"))
                    .header("Content-Type", "application/json") // 设置请求体类型
                    .header("triumphtoken", token)
                    .POST(HttpRequest.BodyPublishers.ofString(JSON.toJSONString(chanmamaQueryBo))) // 发送 JSON 请求体
                    .timeout(Duration.ofSeconds(60)) // 设置请求超时时间
                    .build();

            // 发送请求并获取响应
            try {
                // 解析响应体为字符串
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                if(response.statusCode() == 200) {
                    String resultStr = response.body();
                    R<String> r = JSON.parseObject(resultStr, new TypeReference<R<String>>() {});
                    if(r.getCode() == 0) {
                        r.setData(JSON.toJSONString(r));
                        return r;
                    }else if(r.getCode() == 501 || r.getCode() == 502 || r.getCode() == 504 || r.getCode() == 505) {
                        // token失效，清掉旧token，然后重试
                        redisTemplate.delete(chanmamaProperties.getTokenRedisKey());
                        retryNum --;
                        continue;
                    }else {
                        r.setData(JSON.toJSONString(r));
                        return r;
                    }
                }

                log.warn("查询第三方数据平台数据请求失败：{}======={}", response.body(), JSON.toJSONString(chanmamaQueryBo));

            } catch (Exception e) {
                log.warn("查询第三方数据平台数据发生异常，{}", JSON.toJSONString(chanmamaQueryBo));
                e.printStackTrace();
            }

        }

        return R.error(501, "查询失败");
    }

    /**
     * 获取第三方平台调用token
     * @return
     */
    @CustomRedissonLock(key = "'get_chanmama_token'")
    public String getToken() {

        int retryNum = 3;
        while (retryNum > 0) {
            String token = getCacheToken();
            if(!StringUtils.isEmpty(token)) {
                return token;
            }else {
                // 不存在token，重新登录/刷新
                log.warn("没有获取到第三方数据平台token，5秒后重试===");
                try {
                    Thread.sleep(5000);
                }catch (Exception e) {
                    e.printStackTrace();
                }
                refreshChanmamaToken();
            }
            retryNum--;
        }

        return "";
    }

    private String getCacheToken() {
        Map<Object, Object> entries = this.redisTemplate.opsForHash().entries(chanmamaProperties.getTokenRedisKey());
        if(entries.size() > 0) {
            List<ChanmamaCache> chanmamaCaches = new LinkedList<>();
            for (Map.Entry<Object, Object> entry : entries.entrySet()) {
                ChanmamaCache chanmamaCache = (ChanmamaCache) entry.getValue();
                if(chanmamaCache != null) {
                    chanmamaCaches.add(chanmamaCache);
                }
            }

            if(chanmamaCaches.size() > 0) {
                // 查出剩余次数最多的账号
                chanmamaCaches.sort(Comparator.comparingInt(ChanmamaCache::getRemainingQueryNum).reversed());
                ChanmamaCache chanmamaCache = chanmamaCaches.get(0);
                return chanmamaCache.getToken();
            }
        }

        return null;
    }

    /**
     * 刷新token
     */
    public void refreshChanmamaToken() {

        // 获取所有第三方数据平台账号
        List<ChanmamaAccountEntity> chanmamaAccountEntities = this.chanmamaAccountService.list();

        if(chanmamaAccountEntities != null && chanmamaAccountEntities.size() > 0) {

            // 将不存在于数据库的redis第三方数据平台账号删掉
            Map<Object, Object> entries = this.redisTemplate.opsForHash().entries(chanmamaProperties.getTokenRedisKey());
            if(entries.size() > 0) {
                for (Map.Entry<Object, Object> entry : entries.entrySet()) {
                    Long chanmamaId = Long.valueOf((String) entry.getKey());
                    boolean exist = false;
                    for (ChanmamaAccountEntity chanmamaAccountEntity : chanmamaAccountEntities) {
                        if(chanmamaAccountEntity.getId().equals(chanmamaId)) {
                            exist = true;
                            break;
                        }
                    }
                    if(!exist) {
                        redisTemplate.opsForHash().delete(chanmamaProperties.getTokenRedisKey(), chanmamaId);
                    }
                }
            }

            // 登录或刷新token
            for (ChanmamaAccountEntity chanmamaAccountEntity : chanmamaAccountEntities) {
                // 判断缓存里面是否存在
                Object obj = redisTemplate.opsForHash().get(chanmamaProperties.getTokenRedisKey(), chanmamaAccountEntity.getId());
                if(obj == null) {
                    // 登录获取token
                    String token = login(chanmamaAccountEntity.getUsername(), chanmamaAccountEntity.getChanmamaPassword());
                    if(!StringUtils.isEmpty(token)) {
                        ChanmamaCache chanmamaCache = new ChanmamaCache();
                        chanmamaCache.setId(chanmamaAccountEntity.getId());
                        chanmamaCache.setToken(token);
                        chanmamaCache.setRemainingQueryNum(chanmamaAccountEntity.getEveryDayQueryNum());
                        redisTemplate.opsForHash().put(chanmamaProperties.getTokenRedisKey(), chanmamaAccountEntity.getId(), chanmamaCache);
                    }
                }else {
                    // 刷新token
                    ChanmamaCache chanmamaCache = (ChanmamaCache) obj;
                    String token = refreshToken(chanmamaCache.getToken());
                    if(!StringUtils.isEmpty(token)) {
                        chanmamaCache.setToken(token);
                        redisTemplate.opsForHash().put(chanmamaProperties.getTokenRedisKey(), chanmamaAccountEntity.getId(), chanmamaCache);
                    }
                }
            }

            // 更新过期时间
            redisTemplate.expire(chanmamaProperties.getTokenRedisKey(), Duration.ofMinutes(30));
        }

    }

    /**
     * 发送修正数据请求
     * @param chanmamaRevisionDto 请求数据
     * @return
     */
    public R<Boolean> sendRevisionQuery(ChanmamaRevisionDto chanmamaRevisionDto) {

        int retryNum = 3;
        String resultStr = "";
        while (retryNum > 0) {

            // 获取token
            String token = getToken();

            String jsonBody = JSON.toJSONString(chanmamaRevisionDto);
            // 构建 POST 请求
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(chanmamaProperties.getRequestBaseUrl() + "/api/priceRevision"))
                    .header("Content-Type", "application/json") // 设置请求体类型
                    .header("triumphtoken", token)
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody)) // 发送 JSON 请求体
                    .timeout(Duration.ofSeconds(60)) // 设置请求超时时间
                    .build();

            // 发送请求并获取响应
            try {
                // 解析响应体为字符串
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                if(response.statusCode() == 200) {
                    resultStr = response.body();

                    R<Boolean> r = JSON.parseObject(resultStr, new TypeReference<R<Boolean>>() {});

                    if(r.getCode() == StatusCode.SUCCESS.getCode()) {
                        return r;
                    }else if(Objects.equals(r.getCode(), ChanmamaResponseCodeEnum.CODE_501.getCode()) ||
                            Objects.equals(r.getCode(), ChanmamaResponseCodeEnum.CODE_502.getCode())||
                            Objects.equals(r.getCode(), ChanmamaResponseCodeEnum.CODE_504.getCode())||
                            Objects.equals(r.getCode(), ChanmamaResponseCodeEnum.CODE_505.getCode())) {
                        // token失效，清掉旧token，然后重试
                        redisTemplate.delete(chanmamaProperties.getTokenRedisKey());
                        retryNum --;
                        continue;
                    }else {
                        return r;
                    }
                }

                log.warn("发送第三方修正数据请求失败：{}======={}", response.body(), JSON.toJSONString(chanmamaRevisionDto));

            } catch (Exception e) {
                log.warn("发送第三方修正数据请求发生异常，{}", JSON.toJSONString(chanmamaRevisionDto), e);
            }

            retryNum --;
        }

        return R.error(501, resultStr);
    }

    /**
     * 发送获取相似达人请求
     * @param sendSimilarAnchorDto 请求数据
     * @return
     */
    public R<Boolean> sendSimilarAnchorQuery(SendSimilarAnchorDto sendSimilarAnchorDto) {

        int retryNum = 3;
        String resultStr = "";
        while (retryNum > 0) {

            // 获取token
            String token = getToken();

            String jsonBody = JSON.toJSONString(sendSimilarAnchorDto);
            // 构建 POST 请求
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(chanmamaProperties.getRequestBaseUrl() + "/api/getSimilars"))
                    .header("Content-Type", "application/json") // 设置请求体类型
                    .header("triumphtoken", token)
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody)) // 发送 JSON 请求体
                    .timeout(Duration.ofSeconds(60)) // 设置请求超时时间
                    .build();

            // 发送请求并获取响应
            try {
                // 解析响应体为字符串
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                if(response.statusCode() == 200) {
                    resultStr = response.body();

                    R<Boolean> r = JSON.parseObject(resultStr, new TypeReference<R<Boolean>>() {});

                    if(r.getCode() == StatusCode.SUCCESS.getCode()) {
                        return r;
                    }else if(Objects.equals(r.getCode(), ChanmamaResponseCodeEnum.CODE_501.getCode()) ||
                            Objects.equals(r.getCode(), ChanmamaResponseCodeEnum.CODE_502.getCode())||
                            Objects.equals(r.getCode(), ChanmamaResponseCodeEnum.CODE_504.getCode())||
                            Objects.equals(r.getCode(), ChanmamaResponseCodeEnum.CODE_505.getCode())) {
                        // token失效，清掉旧token，然后重试
                        redisTemplate.delete(chanmamaProperties.getTokenRedisKey());
                        retryNum --;
                        continue;
                    }else {
                        return r;
                    }
                }

                log.warn("发送获取相似达人请求失败：{}======={}", response.body(), JSON.toJSONString(sendSimilarAnchorDto));

            } catch (Exception e) {
                log.warn("发送获取相似达人请求发生异常，{}", JSON.toJSONString(sendSimilarAnchorDto), e);
            }

            retryNum --;
        }

        return R.error(501, resultStr);
    }

    /**
     * 获取第三方榜单信息
     *
     * @param thirdTradeId 第三方榜单ID
     * @param callbackUrl 回调地址
     * @return 榜单信息
     */
    public R<ThirdSalesRankingResult> getSalesRanking(String thirdTradeId, String callbackUrl) {
        if (!isRelease) {
            return R.error(1, "额度不足请充值");
        }
        String requestId = IdUtil.fastSimpleUUID();
        return Retry.runWithRetry((signal) -> {
            log.info("[第三方榜单] query third anchor info.  requestId: {}, 第{}次请求",  requestId, signal);
            // 获取token
            String token = getToken();
            Map<String, Object> param = new HashMap<>();
            param.put("requestId", requestId);
            param.put("categoryId", thirdTradeId);
            param.put("callBack", callbackUrl);
            String jsonBody = JSON.toJSONString(param);
            // 构建 POST 请求
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(chanmamaProperties.getRequestBaseUrl() + "/api/getSalesRanking"))
                .header("Content-Type", "application/json") // 设置请求体类型
                .header("triumphtoken", token)
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody)) // 发送 JSON 请求体
                .build();
            // 发送请求并获取响应
            try {
                log.info("[第三方榜单] query third anchor info. categoryId: {}, requestId: {}, callback: {}", thirdTradeId, requestId, callbackUrl);
                // 解析响应体为字符串
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                if(response.statusCode() == 200) {
                    String resultStr = response.body();
                    R<ThirdSalesRankingResult> r = JSON.parseObject(resultStr, ThirdSalesRankingResultClass);
                    if(r.getCode() == StatusCode.SUCCESS.getCode()) {
                        return r;
                    } else if(Objects.equals(r.getCode(), ChanmamaResponseCodeEnum.CODE_501.getCode()) ||
                        Objects.equals(r.getCode(), ChanmamaResponseCodeEnum.CODE_502.getCode())||
                        Objects.equals(r.getCode(), ChanmamaResponseCodeEnum.CODE_504.getCode())||
                        Objects.equals(r.getCode(), ChanmamaResponseCodeEnum.CODE_505.getCode())) {
                        // token失效，清掉旧token，然后重试
                        redisTemplate.delete(chanmamaProperties.getTokenRedisKey());
                        throw new AuthenticationException(r.getMsg());
                    } else {
                        return r;
                    }
                }
                log.info("[第三方榜单] query third anchor info. categoryId: {}, requestId: {}, callback: {}, response: {}", thirdTradeId, requestId, callbackUrl, response.body());
                throw new RuntimeException(response.body());
            } catch (Exception e) {
                log.warn("[第三方榜单] query third anchor info. categoryId: {}, requestId: {}, callback: {}, response: {}", thirdTradeId, requestId, callbackUrl, e.getMessage());
                throw new RuntimeException(e);
            }
        }, 3);
    }
}
