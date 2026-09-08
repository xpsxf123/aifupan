package com.jiuyu.replay.common.open;

import com.jiuyu.framework.util.EmptyUtil;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 基于环境变量的密钥提供
 *
 * @author HeHui
 * @date 2026-03-03 01:44
 */
public class APISecretEnvProvide implements APISecretProvide {

    private final Map<String, String> secretMap;

    public APISecretEnvProvide(List<OauthClientParameter.APIKeySecret> apiKeySecretList) {
        if (EmptyUtil.isEmpty(apiKeySecretList)) {
            this.secretMap = EmptyUtil.emptyMap();
        } else {
            this.secretMap = apiKeySecretList.stream().filter(key -> EmptyUtil.isNotEmpty(key.getAppId()) && EmptyUtil.isNotEmpty(key.getSecret())).collect(
                Collectors.toMap(OauthClientParameter.APIKeySecret::getAppId, OauthClientParameter.APIKeySecret::getSecret)
            );
        }
    }

    /**
     * 获取密钥
     *
     * @param appId appId
     *
     * @return 密钥
     */
    @Override
    public Optional<String> getSecret(String appId) {
        return Optional.ofNullable(secretMap.get(appId));
    }
}
