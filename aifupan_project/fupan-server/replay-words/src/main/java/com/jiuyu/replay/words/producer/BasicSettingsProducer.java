package com.jiuyu.replay.words.producer;

import com.jiuyu.replay.words.bo.BasicSettingsBo;
import com.jiuyu.replay.words.entity.BasicSettingsEntity;
import com.jiuyu.replay.generic.vo.words.BasicSettingsVo;

import java.util.List;

/**
 * @author ：lujie
 * @description：基础设置Producer接口
 * @date ：2025/1/7
 */
public interface BasicSettingsProducer {

    /**
     * 根据ID查询基础设置
     *
     * @param id ID
     * @return 基础设置实体
     */
    BasicSettingsEntity getById(Long id);

    /**
     * 保存基础设置
     *
     * @param basicSettingsBo 基础设置实体
     * @return 保存后的实体
     */
    BasicSettingsEntity save(BasicSettingsBo basicSettingsBo);

    /**
     * 更新基础设置
     *
     * @param basicSettingsBo 基础设置实体
     * @return 更新结果
     */
    boolean update(BasicSettingsBo basicSettingsBo);

    /**
     * 根据ID删除基础设置
     *
     * @param id ID
     * @return 删除结果
     */
    boolean deleteById(Long id);

    /**
     * 根据来源ID、来源类型、用户ID和租户ID查询单个基础设置
     *
     * @param sourceId   来源ID
     * @param sourceType 来源类型
     * @param userId     用户ID
     * @param tenantId   租户ID
     * @return 基础设置实体
     */
    BasicSettingsVo getBySourceUser(String sourceId, Integer sourceType, Long userId, Long tenantId);

    /**
     * 更新基础设置
     *
     * @param basicSettingsBo 更新基础设置实体
     */
    BasicSettingsBo updateAiPartialNew(BasicSettingsBo basicSettingsBo);

    /**
     * 根据来源ID、来源类型、用户ID和租户ID查询多个基础设置
     *
     * @param sourceIds  来源ID列表
     * @param sourceType 来源类型
     * @param userId     用户ID
     * @param tenantId   租户ID
     * @return 基础设置列表
     */
    List<BasicSettingsVo> listBySourceUser(List<String> sourceIds, Integer sourceType, Long userId, Long tenantId);

    /**
     * 根据来源ID、来源类型、用户ID和租户ID查询多个基础设置
     *
     * @param sourceIds  来源ID列表
     * @param sourceType 来源类型
     * @param userIds    用户ID
     * @param tenantId   租户ID
     * @return 基础设置列表
     */
    List<BasicSettingsVo> listBySourceUser(List<String> sourceIds, Integer sourceType, List<Long> userIds, Long tenantId);
}