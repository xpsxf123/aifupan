package com.jiuyu.replay.agent.constant;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @author ：jxy
 * @description：
 * @date ：2025/4/8 上午11:19
 */
@Component
@Data
@ConfigurationProperties(prefix = "agent")
public class AgentProperties {

    /**
     * h5链接地址
     */
    private String url;
}
