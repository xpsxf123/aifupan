package com.jiuyu.replay.common.http;

import com.jiuyu.framework.util.EmptyUtil;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.support.HttpRequestWrapper;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.retry.Retry;

import java.io.IOException;
import java.net.ConnectException;
import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 自定义负载均衡实例拦截器
 *
 * @author HeHui
 * @date 2026-03-17 16:40
 */
public class LightweightLoadBalancerInterceptor implements ClientHttpRequestInterceptor {


    /**
     * 负载均衡所需的服务ID和实例列表
     */
    private final List<ServiceInstance> serviceInstances;


    /**
     * 总权重
     */
    private final int totalWeight;
    /**
     * 重试策略
     */
    private final Retry retryStrategy;

    /**
     * 负载均衡计数器
     */
    private final AtomicInteger counter = new AtomicInteger(0);


    /**
     * 构造一个新的负载均衡过滤器实例
     *
     * @param serviceInstances 服务实例列表，不能为空
     *
     * @throws IllegalArgumentException 当serviceInstances为空时抛出
     */
    public LightweightLoadBalancerInterceptor(List<ServiceInstance> serviceInstances) {
        this(serviceInstances, null);
    }

    /**
     * 构造一个新的负载均衡过滤器实例
     *
     * @param serviceInstances 服务实例列表，不能为空
     * @param retryStrategy    重试策略，如果为null则使用默认策略
     *
     * @throws IllegalArgumentException 当serviceInstances为空时抛出
     */
    public LightweightLoadBalancerInterceptor(List<ServiceInstance> serviceInstances, Retry retryStrategy) {
        this.serviceInstances = serviceInstances;
        if (EmptyUtil.isEmpty(serviceInstances)) {
            throw new IllegalArgumentException("服务实例列表不能为空");
        }
        this.totalWeight = serviceInstances.stream().mapToInt(ServiceInstance::getWeight).sum();
        this.retryStrategy = Objects.requireNonNullElseGet(retryStrategy, () ->
            Retry.backoff(serviceInstances.size() > 1 ? serviceInstances.size() - 1 : 1, Duration.ofMillis(100))
                .jitter(0.5)
                .filter(throwable -> throwable instanceof ConnectException ||
                    throwable instanceof TimeoutException ||
                    throwable.getMessage().contains("5xx"))
                .onRetryExhaustedThrow((spec, signal) -> new RuntimeException("重试耗尽", signal.failure())));
    }


    /**
     * 拦截 HTTP 请求，执行负载均衡和故障转移逻辑
     * <p>
     * 该方法使用响应式编程模型实现负载均衡：
     * 1. 维护不可用实例列表，在重试时排除故障实例
     * 2. 每次请求（包括重试）都重新选择服务实例并构建新 URI
     * 3. 如果请求失败，将失败实例加入不可用列表并触发重试
     * 4. 根据配置的重试策略进行自动重试
     * </p>
     *
     * @param request   HTTP 请求对象，包含原始请求的所有信息
     * @param body      请求体字节数组
     * @param execution 请求执行器，用于执行实际的 HTTP 请求
     *
     * @return 服务器响应对象
     *
     * @throws IOException 当所有重试都失败或没有可用实例时抛出
     */
    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
        // 封装请求逻辑，重试时重新选择实例
        List<String> unavailable = new ArrayList<>();
        return Objects.requireNonNull(Mono.fromCallable(() -> {
            // 每次请求（包括重试）重新获取实例，构建新 URI
            String newInstance = getNextInstance(unavailable);
            if (EmptyUtil.isEmpty(newInstance)) {
                throw new NoResourceFoundException(request.getMethod(), "没有可用实例");
            }
            HttpRequest newRequest = this.wrapped(request, newInstance);
            try {
                return execution.execute(newRequest, body);
            } catch (IOException e) {
                unavailable.add(newInstance);
                throw e;
            }
        }).retryWhen(retryStrategy).subscribeOn(Schedulers.boundedElastic()).block());
    }


    /**
     * 根据权重轮询算法获取下一个服务实例
     * <p>
     * 该方法实现了加权轮询算法：
     * 1. 当只有一个服务实例时，直接返回该实例
     * 2. 当有多个服务实例时，根据权重分配请求
     * 3. 使用原子计数器保证线程安全
     * </p>
     *
     * @return 下一个服务实例的标识符
     */
    private String getNextInstance(List<String> unavailable) {
        if (serviceInstances.size() == 1) {
            return serviceInstances.get(0).getInstanceId();
        }
        int index = counter.getAndIncrement() % totalWeight;
        if (counter.get() > Integer.MAX_VALUE - 1000) counter.set(0);

        // 按权重区间匹配实例
        int current = 0;
        for (ServiceInstance instance : serviceInstances) {
            if (unavailable.contains(instance.getInstanceId())) {
                continue;
            }
            current += instance.getWeight();
            if (index < current) {
                return instance.getInstanceId();
            }
        }
        return serviceInstances.get(0).getInstanceId(); // 兜底
    }


    /**
     * 创建一个包装后的HttpRequest对象，用于替换原始请求的URI
     *
     * @param originalRequest 原始请求对象
     * @param newInstance     新的服务实例标识符
     *
     * @return 包装后的HttpRequest对象
     */
    private HttpRequest wrapped(HttpRequest originalRequest, String newInstance) {
        String[] split = newInstance.split("://");
        String host = split.length == 2 ? split[1] : newInstance;
        String[] hostSplit = host.split(":");
        if (hostSplit.length == 2) {
            host = hostSplit[0];
        }
        int port = hostSplit.length == 2 ? Integer.parseInt(hostSplit[1]) : Objects.equals(split[0], "https") ? 443 : 80;
        URI newUri = UriComponentsBuilder.fromUri(originalRequest.getURI())
            .host(host)
            .port(port)
            .build().toUri();
        return new HttpRequestWrapper(originalRequest) {
            @Override
            public URI getURI() {
                return newUri;
            }
        };
    }
}
