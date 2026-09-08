package com.jiuyu.replay.api.service.agentusage.impl;

import com.alibaba.fastjson2.JSON;
import com.google.common.collect.Lists;
import com.jiuyu.replay.api.config.AgentUsageProperties;
import com.jiuyu.replay.api.service.agentusage.AgentUsageService;
import com.jiuyu.replay.api.service.agentusage.dto.AgentUsage;
import com.jiuyu.replay.api.service.agentusage.dto.AgentUsageApiResp;
import com.jiuyu.replay.api.service.agentusage.dto.UserTenantPair;
import com.jiuyu.replay.common.cache.ResilientRedisTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 智能体用量统计实现：实时调外部接口 + 本地缓存 5 分钟 + 失败降级(不重试)。
 * - 租户维度：接口1 /open/stats/tenant/usage（ids 字符串）。
 * - 用户维度：接口5 /open/stats/user-tenant/usage（pairs，userId/tenantId 走 number），
 *   只统计用户在其当前租户(active_tenant_id)下的用量，避免跨租户糊成一笔。
 * 缓存粒度为单个 id / 单个(user,tenant)对，跨页复用；窗口滚动(now-60d)但 TTL 仅 5 分钟，窗口漂移可忽略。
 */
@Slf4j
@Service
public class AgentUsageServiceImpl implements AgentUsageService {

    private static final String TENANT_PATH = "/open/stats/tenant/usage";
    private static final String USER_TENANT_PATH = "/open/stats/user-tenant/usage";
    private static final String CACHE_KEY_PREFIX = "replay:agentusage:v1:";
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final AgentUsageProperties props;
    private final ResilientRedisTemplate<String, Object> cache;
    private final HttpClient httpClient;

    public AgentUsageServiceImpl(AgentUsageProperties props, ResilientRedisTemplate<String, Object> cache) {
        this.props = props;
        this.cache = cache;
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofMillis(props.getConnectTimeoutMs()))
                .build();
    }

    @Override
    public Map<Long, AgentUsage> batchTenantUsage(Collection<Long> tenantIds) {
        Map<Long, AgentUsage> result = new HashMap<>();
        if (tenantIds == null || tenantIds.isEmpty()) {
            return result;
        }
        List<Long> validIds = tenantIds.stream()
                .filter(id -> id != null && id > 0)
                .distinct()
                .collect(Collectors.toList());
        if (validIds.isEmpty()) {
            return result;
        }
        // 1) 查缓存，收集未命中
        List<Long> missing = new ArrayList<>();
        for (Long id : validIds) {
            AgentUsage cached = readCache(tenantCacheKey(id));
            if (cached != null) {
                result.put(id, cached);
            } else {
                missing.add(id);
            }
        }
        // 2) 未命中分批调接口1
        if (!missing.isEmpty()) {
            String startTime = windowStart();
            for (List<Long> partition : Lists.partition(missing, props.getMaxIdsPerBatch())) {
                result.putAll(fetchTenantBatch(partition, startTime));
            }
        }
        // 3) 兜底：每个 id 都要有值
        for (Long id : validIds) {
            result.putIfAbsent(id, AgentUsage.zero());
        }
        return result;
    }

    @Override
    public Map<String, AgentUsage> batchUserTenantUsage(Collection<UserTenantPair> pairs) {
        Map<String, AgentUsage> result = new HashMap<>();
        if (pairs == null || pairs.isEmpty()) {
            return result;
        }
        // 去重(按 key) + 去 userId/tenantId 空或非正
        LinkedHashMap<String, UserTenantPair> validPairs = new LinkedHashMap<>();
        for (UserTenantPair p : pairs) {
            if (p == null || p.getUserId() == null || p.getUserId() <= 0
                    || p.getTenantId() == null || p.getTenantId() <= 0) {
                continue;
            }
            validPairs.putIfAbsent(p.key(), p);
        }
        if (validPairs.isEmpty()) {
            return result;
        }
        // 1) 查缓存，收集未命中
        List<UserTenantPair> missing = new ArrayList<>();
        for (UserTenantPair p : validPairs.values()) {
            AgentUsage cached = readCache(userTenantCacheKey(p.getUserId(), p.getTenantId()));
            if (cached != null) {
                result.put(p.key(), cached);
            } else {
                missing.add(p);
            }
        }
        // 2) 未命中分批调接口5
        if (!missing.isEmpty()) {
            String startTime = windowStart();
            for (List<UserTenantPair> partition : Lists.partition(missing, props.getMaxIdsPerBatch())) {
                result.putAll(fetchUserTenantBatch(partition, startTime));
            }
        }
        // 3) 兜底：每对都要有值
        for (UserTenantPair p : validPairs.values()) {
            result.putIfAbsent(p.key(), AgentUsage.zero());
        }
        return result;
    }

    /**
     * 调一批接口1（租户 ids）。成功回填缓存并返回；任何异常/非0/超时 → 整批降级为0（不写缓存、不重试）。
     */
    private Map<Long, AgentUsage> fetchTenantBatch(List<Long> idBatch, String startTime) {
        Map<Long, AgentUsage> batchResult = new HashMap<>();
        try {
            List<String> idStrings = idBatch.stream().map(String::valueOf).collect(Collectors.toList());
            Map<String, Object> body = new HashMap<>();
            body.put("ids", idStrings);
            body.put("startTime", startTime);

            AgentUsageApiResp resp = postJson(TENANT_PATH, body);
            if (!isUsable(resp)) {
                log.warn("智能体用量接口1非成功响应，降级为0. size={}, code={}",
                        idBatch.size(), resp == null ? null : resp.getCode());
                return degradeTenant(idBatch);
            }
            for (AgentUsageApiResp.UsageItem item : resp.getData().getItems()) {
                Long id = parseLong(item.getId());
                if (id == null) {
                    continue;
                }
                AgentUsage usage = toUsage(item);
                batchResult.put(id, usage);
                writeCache(tenantCacheKey(id), usage);
            }
            return batchResult;
        } catch (Exception e) {
            log.warn("智能体用量接口1调用异常，降级为0. size={}, err={}", idBatch.size(), e.getMessage());
            return degradeTenant(idBatch);
        }
    }

    /**
     * 调一批接口5（用户×租户 pairs）。成功回填缓存并返回；任何异常/非0/超时 → 整批降级为0（不写缓存、不重试）。
     */
    private Map<String, AgentUsage> fetchUserTenantBatch(List<UserTenantPair> pairBatch, String startTime) {
        Map<String, AgentUsage> batchResult = new HashMap<>();
        try {
            List<Map<String, Object>> pairMaps = new ArrayList<>();
            for (UserTenantPair p : pairBatch) {
                Map<String, Object> m = new HashMap<>();
                m.put("userId", p.getUserId());   // number
                m.put("tenantId", p.getTenantId());
                pairMaps.add(m);
            }
            Map<String, Object> body = new HashMap<>();
            body.put("pairs", pairMaps);
            body.put("startTime", startTime);

            AgentUsageApiResp resp = postJson(USER_TENANT_PATH, body);
            if (!isUsable(resp)) {
                log.warn("智能体用量接口5非成功响应，降级为0. size={}, code={}",
                        pairBatch.size(), resp == null ? null : resp.getCode());
                return degradePair(pairBatch);
            }
            for (AgentUsageApiResp.UsageItem item : resp.getData().getItems()) {
                if (item.getUserId() == null || item.getTenantId() == null) {
                    continue;
                }
                String key = UserTenantPair.key(item.getUserId(), item.getTenantId());
                AgentUsage usage = toUsage(item);
                batchResult.put(key, usage);
                writeCache(userTenantCacheKey(item.getUserId(), item.getTenantId()), usage);
            }
            return batchResult;
        } catch (Exception e) {
            log.warn("智能体用量接口5调用异常，降级为0. size={}, err={}", pairBatch.size(), e.getMessage());
            return degradePair(pairBatch);
        }
    }

    /**
     * 发一次 POST（带鉴权头、读超时），解析为信封。异常向上抛由调用方降级。
     */
    private AgentUsageApiResp postJson(String path, Map<String, Object> body) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(props.getBaseUrl() + path))
                .timeout(Duration.ofMillis(props.getReadTimeoutMs()))
                .header("Content-Type", "application/json")
                .header("x-jiuyu-client-id", props.getAppId())
                .header("api-key", props.getApiKey())
                .POST(HttpRequest.BodyPublishers.ofString(JSON.toJSONString(body)))
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        return JSON.parseObject(response.body(), AgentUsageApiResp.class);
    }

    private boolean isUsable(AgentUsageApiResp resp) {
        return resp != null && resp.isSuccess() && resp.getData() != null && resp.getData().getItems() != null;
    }

    private AgentUsage toUsage(AgentUsageApiResp.UsageItem item) {
        return new AgentUsage(
                item.getBilledTokens() == null ? 0L : item.getBilledTokens(),
                item.getUserMessageCount() == null ? 0L : item.getUserMessageCount());
    }

    private Map<Long, AgentUsage> degradeTenant(List<Long> idBatch) {
        Map<Long, AgentUsage> zero = new HashMap<>();
        for (Long id : idBatch) {
            zero.put(id, AgentUsage.zero());
        }
        return zero;
    }

    private Map<String, AgentUsage> degradePair(List<UserTenantPair> pairBatch) {
        Map<String, AgentUsage> zero = new HashMap<>();
        for (UserTenantPair p : pairBatch) {
            zero.put(p.key(), AgentUsage.zero());
        }
        return zero;
    }

    private String windowStart() {
        return LocalDateTime.now().minusDays(props.getWindowDays()).format(TIME_FMT);
    }

    private Long parseLong(String id) {
        if (id == null || id.isEmpty()) {
            return null;
        }
        try {
            return Long.parseLong(id.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String tenantCacheKey(Long tenantId) {
        return CACHE_KEY_PREFIX + "tenant:" + tenantId;
    }

    private String userTenantCacheKey(Long userId, Long tenantId) {
        return CACHE_KEY_PREFIX + "usertenant:" + userId + ":" + tenantId;
    }

    private AgentUsage readCache(String key) {
        try {
            Object cached = cache.opsForValue().get(key);
            if (cached instanceof String json && !json.isEmpty()) {
                return JSON.parseObject(json, AgentUsage.class);
            }
        } catch (Exception e) {
            log.warn("智能体用量缓存读取失败. key={}, err={}", key, e.getMessage());
        }
        return null;
    }

    private void writeCache(String key, AgentUsage usage) {
        try {
            cache.opsForValue().set(key, JSON.toJSONString(usage), props.getCacheTtlSeconds(), TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("智能体用量缓存写入失败. key={}, err={}", key, e.getMessage());
        }
    }
}
