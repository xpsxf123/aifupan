package com.jiuyu.governance.plugins.webmvc;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.io.Serial;
import java.io.Serializable;
import java.time.Duration;

/**
 * Apache HttpClient 5的连接池配置
 *
 * @author HeHui
 * @date 2026-02-25 14:30
 */
@Getter
@Setter
@ConfigurationProperties(prefix = HttpClientPoolProperties.PREFIX)
public class HttpClientPoolProperties implements Serializable {

    public final static String PREFIX = "jiuyu.http.client";

    @Serial
    private static final long serialVersionUID = -4050160963034566340L;

    /** 是否启用 */
    private boolean enabled = true;

    /** 最大总连接数 */
    private int maxTotal = 200;
    /** 每个路由（域名）的最大并发连接数 */
    private int defaultMaxPerRoute = 50;
    /** 建立 TCP 连接的超时时间 */
    private Duration connectTimeout = Duration.ofSeconds(2);
    /** 数据传输（读取）的超时时间 */
    private Duration socketTimeout = Duration.ofSeconds(10);
    /** 从连接池获取连接的等待超时时间 */
    private Duration connectionRequestTimeout = Duration.ofSeconds(1);
    /** 连接在池中的最长生存时间 (TTL) */
    private Duration timeToLive = Duration.ofMinutes(30);
    /** 空闲连接被清理前的最大闲置时间 */
    private Duration maxIdleTime = Duration.ofSeconds(30);
    /** 检查永久连接是否可用的不活跃周期 */
    private Duration validateAfterInactivity = Duration.ofSeconds(5);
}
