package com.jiuyu.replay.third.constant;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author ：lujie
 * @description：
 * @date ：2025/7/7 下午2:59
 */
@Component
@Data
@ConfigurationProperties(prefix = "third.lian-lu")
public class LianLuProperties {

    private List<LianLuMsg> msg;

    @Data
    public static class LianLuMsg {
        /**
         * 应用 ID，用于身份认证。
         */
        private String appId;

        /**
         * 应用密钥，用于签名生成。
         */
        private String appSecret;

        /**
         * 商户号，用于标识发送方。
         */
        private String mchId;

        /**
         * 自定义id
         */
        private Integer id;
    }
}
