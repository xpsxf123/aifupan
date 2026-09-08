package com.jiuyu.replay.common.open;


import java.util.Optional;

/**
 * api key的密钥提供
 *
 * @author HeHui
 * @date 2026-03-03 01:25
 */
public interface APISecretProvide {

    /**
     * 获取密钥
     *
     * @param appId appId
     * @return 密钥
     */
    Optional<String> getSecret(String appId);
}
