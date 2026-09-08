package com.jiuyu.replay.common.tencent;

import com.alibaba.fastjson2.JSONObject;
import com.jiuyu.replay.common.constant.AudioSecretProperties;
import com.jiuyu.replay.common.constant.TencentAudioProperties;
import com.jiuyu.replay.common.utils.SnowflakeManager;
import com.jiuyu.replay.common.vo.CheckSurplusVo;
import com.jiuyu.replay.common.vo.TencentTempTokenVo;
import com.tencentcloudapi.common.Credential;
import com.tencentcloudapi.common.exception.TencentCloudSDKException;
import com.tencentcloudapi.sts.v20180813.models.Credentials;
import com.tencentcloudapi.sts.v20180813.models.GetFederationTokenRequest;
import com.tencentcloudapi.sts.v20180813.models.GetFederationTokenResponse;
import jakarta.annotation.Resource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

@Component
public class TencentAudioUtils {

    private final ReentrantLock lock = new ReentrantLock();

    @Resource
    private RedisTemplate<String, Object> redisTemplate;
    @Resource
    private TencentAudioProperties tencentAudioProperties;

    /**
     * 查询是否还有调用语音识别接口的余量
     * @param userId 用户id
     * @return 有余量返回一个雪花id，后续需要根据这个id来加回余量
     */
    public CheckSurplusVo checkSurplus(Long userId) {
        // 加锁
        lock.lock();
        try {
            // 获取所有语音识别secret，用于判断哪一个还有余量
            List<AudioSecretProperties> secret = tencentAudioProperties.getSecret();

            for (AudioSecretProperties audioSecretProperties : secret) {
                // 查redis，是否还有调用语音识别接口的余量
                Integer number = (Integer) redisTemplate.opsForValue().get(tencentAudioProperties.getRedisKeyPrefix() + audioSecretProperties.getSecretId());
                if(number > 0) {
                    // 减一个余量
                    number -= 1;
                    redisTemplate.opsForValue().set(tencentAudioProperties.getRedisKeyPrefix() + audioSecretProperties.getSecretId(), number);

                    // redis记录日志，定时任务插入数据库
                    String logStr = userId + "-" + new Date().getTime();
                    redisTemplate.opsForValue().set(tencentAudioProperties.getLogKeyPrefix() + logStr, logStr);

                    // 加一个信号到redis，监听到过期，就加回余量，避免因客户原因没有通知加回余量
                    Long flag = SnowflakeManager.nextValue();
                    CheckSurplusVo checkSurplusVo = new CheckSurplusVo();
                    checkSurplusVo.setId(flag);
                    checkSurplusVo.setSecretId(audioSecretProperties.getSecretId());
                    redisTemplate.opsForValue().set(tencentAudioProperties.getRedisTimeoutKeyPrefix() + flag + "-" + audioSecretProperties.getSecretId(), 1, Duration.ofSeconds(30));

                    return checkSurplusVo;
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // 解锁
            lock.unlock();
        }

        return null;
    }

    /**
     * 通知加回语音识别接口的余量
     * @param id 补回余量的id
     * @param secretId 补回余量的secretId
     * @return
     */
    public synchronized void addSurplus(Long id, String secretId) {

        if(Boolean.TRUE.equals(redisTemplate.hasKey(tencentAudioProperties.getRedisTimeoutKeyPrefix() + id + "-" + secretId))) {
            // 删除key
            redisTemplate.delete(tencentAudioProperties.getRedisTimeoutKeyPrefix() + id + "-" + secretId);

            List<AudioSecretProperties> secret = tencentAudioProperties.getSecret();
            for (AudioSecretProperties audioSecretProperties : secret) {
                if(audioSecretProperties.getSecretId().equals(secretId)) {
                    Integer number = (Integer) redisTemplate.opsForValue().get(tencentAudioProperties.getRedisKeyPrefix() + secretId);
                    if(number < 30) {
                        number += 1;
                        redisTemplate.opsForValue().set(tencentAudioProperties.getRedisKeyPrefix() + secretId, number);
                    }
                }
            }

        }
    }


    /**
     * 获取语音识别接口临时调用凭证
     * @param userId 用户id
     * @param secretId 语音识别secretId
     * @param action 动作  asr:CreateRecTask》录音文件识别  asr:SentenceRecognition》一句话识别
     */
    public TencentTempTokenVo getTempToken(Long userId, String secretId, String action) throws TencentCloudSDKException {

        List<AudioSecretProperties> secret = tencentAudioProperties.getSecret();
        if(StringUtils.isEmpty(secretId)) {
            int random = (int) (Math.random() * secret.size());
            AudioSecretProperties audioSecretProperties = secret.get(random);
            secretId = audioSecretProperties.getSecretId();
        }

        // 查缓存，如果有，直接返回
        Object obj = redisTemplate.opsForValue().get(tencentAudioProperties.getRedisTempTokenKeyPrefix() + userId + action + secretId);
        if(obj != null) {
            return  (TencentTempTokenVo) obj;
        }

        Credential credential = null;
        for (AudioSecretProperties audioSecretProperties : secret) {
            if(audioSecretProperties.getSecretId().equals(secretId)) {
                credential = new Credential(audioSecretProperties.getSecretId(), audioSecretProperties.getSecretKey());
            }
        }


        TencentStsClient tencentStsClient = new TencentStsClient(credential, "ap-shanghai");
        GetFederationTokenRequest getFederationTokenRequest = new GetFederationTokenRequest();
        getFederationTokenRequest.setName("jiuyu"); // 调用方名称，可自定义
        getFederationTokenRequest.setDurationSeconds(7200L); // 指定临时证书的有效期，单位：秒，默认1800秒

        // 构建Policy权限策略参数
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("version", "2.0"); // 描述策略语法版本。该元素是必填项。目前仅允许值为“2.0”。
        LinkedList<HashMap<String, Object>> statementList = new LinkedList<>(); // 描述一条或多条权限的详细信息

        HashMap<String, Object> statementMap = new HashMap<>(); // 描述策略授权的实体
        statementMap.put("effect", "allow"); // 描述声明产生的结果是“允许”还是“显式拒绝”。包括 allow（允许）和 deny （显式拒绝）两种情况。该元素是必填项。
        statementMap.put("resource", "*"); // 为 * 时代表授予所有资源的操作权限
        statementMap.put("action", List.of(action)); // 描述允许或拒绝的操作
        statementList.add(statementMap);

        jsonObject.put("statement", statementList);
        getFederationTokenRequest.setPolicy(jsonObject.toJSONString());

        // 获取到临时凭证
        GetFederationTokenResponse federationToken = tencentStsClient.getFederationToken(getFederationTokenRequest);
        Credentials credentials = federationToken.getCredentials();

        TencentTempTokenVo audioTempTokenVo = new TencentTempTokenVo();
        audioTempTokenVo.setToken(credentials.getToken());
        audioTempTokenVo.setTempSecretId(credentials.getTmpSecretId());
        audioTempTokenVo.setTempSecretKey(credentials.getTmpSecretKey());

        // 存到缓存
        redisTemplate.opsForValue().set(tencentAudioProperties.getRedisTempTokenKeyPrefix() + userId + action + secretId, audioTempTokenVo, Duration.ofSeconds(1500));

        return audioTempTokenVo;

    }

    /**
     * 记录qps访问
     * @param userId 用户id
     */
    public void record(Long userId) {

        String logStr = userId + "-" + new Date().getTime();
        redisTemplate.opsForValue().set(tencentAudioProperties.getQpsLogKeyPrefix() + logStr, logStr);

    }



}
