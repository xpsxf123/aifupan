package com.jiuyu.replay.ai.rse;

import com.jiuyu.replay.ai.bo.AiPlaceholderBo;
import com.jiuyu.replay.ai.bo.AiPlaceholderListBo;
import com.jiuyu.replay.ai.vo.AiPlaceholderVo;
import com.jiuyu.replay.generic.utils.PageUtils;

import java.util.List;

/**
 * AI占位符配置 Rse
 *
 * @author jy
 * @date 2026-06-16
 */
public interface AiPlaceholderRse {

    PageUtils<AiPlaceholderVo> queryPage(AiPlaceholderListBo listBo);

    AiPlaceholderVo info(Long id);

    AiPlaceholderVo save(AiPlaceholderBo bo);

    void update(AiPlaceholderBo bo);

    void deleteById(Long id);

    List<AiPlaceholderVo> listAll();

    /**
     * 客户端获取占位符配置列表（前端展示、未删除、已启用，按 sort 升序）
     */
    List<AiPlaceholderVo> listForClient();

    /**
     * 按完整 key 查单个占位符（如 "#{trade}"）
     */
    AiPlaceholderVo getByKey(String fullKey);

    /**
     * 按完整 key 列表批量查占位符
     */
    List<AiPlaceholderVo> listByKeys(List<String> fullKeys);
}
