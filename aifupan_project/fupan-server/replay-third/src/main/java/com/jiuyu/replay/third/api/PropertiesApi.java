package com.jiuyu.replay.third.api;

import com.jiuyu.replay.generic.feign.third.PropertiesFeign;
import com.jiuyu.replay.generic.vo.third.VolcengineVo;
import com.jiuyu.replay.third.constant.VolcengineProperties;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * @author ：lujie
 * @description：
 * @date ：2025/6/3 下午1:49
 */
@Component
@AllArgsConstructor
public class PropertiesApi implements PropertiesFeign {

    private final VolcengineProperties volcengineProperties;

    @Override
    public VolcengineVo getVolcengine() {
        return volcengineProperties;
    }
}
