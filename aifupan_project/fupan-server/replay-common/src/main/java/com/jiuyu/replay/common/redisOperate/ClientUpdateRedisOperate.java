package com.jiuyu.replay.common.redisOperate;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.jiuyu.replay.common.entity.ClientUpdateEntity;
import jakarta.annotation.Resource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

/**
 * @author ：lujie
 * @description：
 * @date ：2025/12/8 15:31
 */
@Component
public class ClientUpdateRedisOperate {

    @Resource
    private RedisTemplate<String, String> redisTemplate;

    public static final String KEY = "replay:version-update-all";

    /**
     * 删除
     *
     * @return 删除成功返回true
     */
    public boolean delete() {
        try {
            redisTemplate.delete(KEY);
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    /**
     * 保存
     *
     * @param list 数据对象
     * @return 保存成功返回true
     */
    public boolean save(List<ClientUpdateEntity> list) {
        try {
            redisTemplate.opsForValue().set(KEY, JSONObject.toJSONString(list), Duration.ofDays(2));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 获取
     *
     * @return 数据对象
     */
    public List<ClientUpdateEntity> get() {
        try {
            String json = redisTemplate.opsForValue().get(KEY);
            return JSONArray.parseArray(json, ClientUpdateEntity.class);
        } catch (Exception e) {
            return null;
        }
    }


}
