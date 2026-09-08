package com.jiuyu.replay.generic.feign.system;

import com.github.xiaoymin.knife4j.core.util.StrUtil;
import com.jiuyu.framework.json.JsonTemplate;
import com.jiuyu.framework.util.DateOps;
import com.jiuyu.framework.util.EmptyUtil;
import com.jiuyu.replay.generic.vo.ai.AiContentCorrectionConfigVo;
import com.jiuyu.replay.generic.vo.system.DictDataListVo;
import org.apache.commons.lang3.math.NumberUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * 字典的接口
 */
public interface DictDataFeign {

    /**
     * 根据字典类型标识和字典值查询字典信息
     *
     * @param code  字典类型标识
     * @param value 字典值
     *
     * @return 字段信息
     */
    DictDataListVo dictDataByValue(String code, String value);

    /**
     * 根据字典label询字典信息
     *
     * @param code  字典类型标识
     * @param label label
     *
     * @return 字段信息
     */
    DictDataListVo dictDataByLabel(String code, String label);

    /**
     * 根据字典类型标识查询字典信息
     *
     * @param code 字典类型标识
     *
     * @return 字段信息
     */
    List<DictDataListVo> dictDataListByCode(String code);

    /**
     * 根据字典类型标识查询字典信息
     *
     * @param code 字典类型标识
     * @return 字段信息 label结构为 父/子/当前
     */
    List<DictDataListVo> dictDataParentLabelByCode(String code);

    /**
     * 根据字典类型标识查询字典信息
     *
     * @param code 字典类型标识
     * @return 字段信息
     */
    List<DictDataListVo> dictDataTreeListByCode(String code);


    /**
     * 根据场景类型获取AI内容校正配置（模型code和提示词）
     *
     * @param sceneType 场景类型 0-AI问答助手纠正检查 1-AI问答助手纠正 2-自然/优化原文纠正检查 3-自然/优化原文纠正
     * @return 配置信息，未找到时返回 null
     */
    AiContentCorrectionConfigVo getContentCorrectionConfig(Integer sceneType);

    /**
     * 根据字典类型标识查询字典信息
     *
     * @param ids ids
     * @return 字段信息
     */
    List<DictDataListVo> dictDataListByIds(List<Long> ids);


    /**
     * 获取配置值
     *
     * @param key   名称
     * @param label 标签
     *
     * @return {@link String}
     */
    default String getConfigValue(String key, String label) {
        if (EmptyUtil.isEmpty(key) || EmptyUtil.isEmpty(label)) {
            return null;
        }
        DictDataListVo dictDataListVo = dictDataByLabel(key, label);
        if (EmptyUtil.isEmpty(dictDataListVo)) {
            return null;
        }
        return dictDataListVo.getValue();
    }

    /**
     * 获取配置值 如果没有取默认值
     *
     * @param key          字典类型标识
     * @param label        标签
     * @param defaultValue 默认值
     *
     * @return {@link String}
     */
    default String getValueDefault(String key, String label, String defaultValue) {
        String value = getConfigValue(key, label);
        if (StrUtil.isBlank(value)) {
            return defaultValue;
        }
        return value;
    }


    /**
     * 获取日期
     *
     * @param key   字典类型标识
     * @param label 标签
     *
     * @return {@link Optional }<{@link LocalDateTime }>
     */
    default Optional<LocalDateTime> getDate(String key, String label) {
        String value = getConfigValue(key, label);
        if (StrUtil.isBlank(value)) {
            return Optional.empty();
        }
        try {
            // if milliseconds ??
            if (NumberUtils.isCreatable(value)) {
                return Optional.of(DateOps.of(Long.parseLong(value)).getLocalDateTime());
            }
            return Optional.of(DateOps.of(value, DateTimeFormatter.ISO_LOCAL_DATE_TIME).getLocalDateTime());
        } catch (Exception e) {
            return Optional.empty();
        }
    }


    /**
     * 获取配置值
     *
     * @param key   名称
     * @param label 标签
     * @param clazz 类型
     *
     * @return {@link Optional}<{@link T}>
     */
    default <T> Optional<T> getConfigValue(String key, String label, Class<T> clazz) {
        String body = getConfigValue(key, label);
        if (!JsonTemplate.isJsonObj(body)) {
            return Optional.empty();
        }
        return Optional.ofNullable(JsonTemplate.toBean(body, clazz));
    }

    /**
     * 获取配置值
     *
     * @param key   名称
     * @param label 标签
     * @param clazz 类型
     *
     * @return {@link List}<{@link T}>
     */
    default <T> List<T> getConfigList(String key, String label, Class<T> clazz) {
        String body = getConfigValue(key, label);
        if (!JsonTemplate.isJsonArray(body)) {
            return Collections.emptyList();
        }
        return JsonTemplate.toList(body, clazz);
    }
}
