package com.jiuyu.replay.third.sms.config;

import cn.hutool.core.util.ObjectUtil;
import com.jiuyu.replay.common.entity.SystemKvEntity;
import com.jiuyu.replay.common.repository.service.SystemKvService;
import com.jiuyu.replay.generic.feign.system.DictDataFeign;
import com.jiuyu.replay.generic.vo.system.DictDataListVo;
import com.jiuyu.replay.generic.vo.system.DictDataVo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * @author ：lujie
 * @description：
 * @date ：2025/6/20 上午11:28
 */
@Component
public class SmsConfig {

    @Resource
    private SystemKvService systemKvService;
    @Resource
    private DictDataFeign dictDataFeign;

    /**
     * 是否启用短信服务
     */
    public boolean isEnabled() {
        SystemKvEntity byKey = systemKvService.getByKey("sms_enabled");
        if (byKey != null) {
            return ObjectUtil.equal(byKey.getKvValue(), "1");
        }

        return true;
    }

    /**
     * 默认使用的短信服务提供商
     */
    public String getDefaultProvider() {
        List<String> fallbackProviders1 = getFallbackProviders();
        if (ObjectUtil.isNotEmpty(fallbackProviders1)) {
            return fallbackProviders1.get(0);
        }
        return "ali";
    }

    /**
     * 备用短信服务提供商列表，按顺序尝试
     */
    public List<String> getFallbackProviders() {

        List<DictDataListVo> dictDataListVos = dictDataFeign.dictDataListByCode("sms_service_providers");
        // 移除禁用的
        dictDataListVos.removeIf(dictDataListVo -> !ObjectUtil.equals(dictDataListVo.getStatus(), 0));
        if (ObjectUtil.isNotEmpty(dictDataListVos)) {
            return dictDataListVos.stream().map(DictDataVo::getValue).distinct().toList();
        }

        return Arrays.asList("ali", "lianLu");
    }
}
