package com.jiuyu.replay.common.open;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.http.HttpHeaders;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 认证授权客户端参数配置
 *
 * @author HeHui
 * @date 2026-02-24 22:15
 */
@ConfigurationProperties("jiuyu.oauth.client")
public class OauthClientParameter implements Serializable {

    @Serial
    private static final long serialVersionUID = -4758950552939512265L;




    /**
     * api-key认证授权密钥
     */
    private List<APIKeySecret> apiKeySecret = new ArrayList<>();




    public List<APIKeySecret> getApiKeySecret() {
        return apiKeySecret;
    }

    public void setApiKeySecret(List<APIKeySecret> apiKeySecret) {
        this.apiKeySecret = apiKeySecret;
    }

    /**
     * api-key认证授权密钥
     *
     * @author HeHui
     * @date 2026/03/03
     */
    public static class APIKeySecret {

        /**
         * 应用ID
         */
        private String appId;

        /**
         * 应用密钥
         */
        private String secret;

        public String getAppId() {
            return appId;
        }

        public void setAppId(String appId) {
            this.appId = appId;
        }

        public String getSecret() {
            return secret;
        }

        public void setSecret(String secret) {
            this.secret = secret;
        }
    }
}
