package com.jiuyu.replay.activity.constant;

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
@ConfigurationProperties(prefix = "activity")
public class ActivityProperties {

    /**
     * 客户端默认邀请活动id
     */
    private Long clientDefaultInviteActivityId;
}
