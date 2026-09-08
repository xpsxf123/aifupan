package com.jiuyu.replay.words.producer.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import com.jiuyu.replay.common.annotation.CustomRedissonLock;
import com.jiuyu.replay.common.utils.SnowflakeManager;
import com.jiuyu.replay.words.bo.BasicSettingsBo;
import com.jiuyu.replay.words.entity.BasicSettingsEntity;
import com.jiuyu.replay.words.producer.BasicSettingsProducer;
import com.jiuyu.replay.words.repository.service.BasicSettingsService;
import com.jiuyu.replay.generic.vo.words.BasicSettingsVo;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;

/**
 * @author ：lujie
 * @description：基础设置Producer实现类
 * @date ：2025/1/7
 */
@Component
@AllArgsConstructor
public class BasicSettingsProducerImpl implements BasicSettingsProducer {

    private final BasicSettingsService basicSettingsService;

    @Override
    public BasicSettingsEntity getById(Long id) {
        return basicSettingsService.getById(id);
    }

    @Override
    public BasicSettingsEntity save(BasicSettingsBo basicSettingsBo) {
        basicSettingsBo.setId(SnowflakeManager.nextValue());
        Date now = new Date();
        basicSettingsBo.setCreateDate(now);
        basicSettingsBo.setUpdateDate(now);
        BasicSettingsEntity basicSettingsEntity = BeanUtil.copyProperties(basicSettingsBo, BasicSettingsEntity.class);
        basicSettingsService.save(basicSettingsEntity);
        return basicSettingsEntity;
    }

    @Override
    public boolean update(BasicSettingsBo basicSettingsBo) {
        basicSettingsBo.setUpdateDate(new Date());
        basicSettingsBo.setCreateDate(null);
        basicSettingsBo.setUserId(null);
        basicSettingsBo.setTenantId(null);
        BasicSettingsEntity basicSettingsEntity = BeanUtil.copyProperties(basicSettingsBo, BasicSettingsEntity.class);
        return basicSettingsService.updateById(basicSettingsEntity);
    }

    @Override
    public boolean deleteById(Long id) {
        return basicSettingsService.removeById(id);
    }

    @Override
    public BasicSettingsVo getBySourceUser(String sourceId, Integer sourceType, Long userId, Long tenantId) {
        BasicSettingsEntity entity = basicSettingsService.lambdaQuery()
                .eq(BasicSettingsEntity::getSourceId, sourceId)
                .eq(BasicSettingsEntity::getSourceType, sourceType)
                .eq(BasicSettingsEntity::getUserId, userId)
                .eq(BasicSettingsEntity::getTenantId, tenantId)
                .last("limit 1")
                .one();
        if (entity != null) {
            return BeanUtil.copyProperties(entity, BasicSettingsVo.class);
        }
        return null;
    }

    @Override
    @CustomRedissonLock(key = "'updateAiPartialNew:' + #args[0]?.sourceId ?: 'null' + '_' + #args[0]?.userId ?: 'null' + '_' + #args[0]?.tenantId ?: 'null'")
    public BasicSettingsBo updateAiPartialNew(BasicSettingsBo basicSettingsBo) {

        BasicSettingsVo basicSettingsVo = getBySourceUser(basicSettingsBo.getSourceId(), basicSettingsBo.getSourceType(), basicSettingsBo.getUserId(), basicSettingsBo.getTenantId());
        if (basicSettingsVo != null) {
            basicSettingsBo.setId(basicSettingsVo.getId());
        }

        if (basicSettingsBo.getId() == null) {
            save(basicSettingsBo);
        } else {
            update(basicSettingsBo);
        }
        return basicSettingsBo;
    }

    @Override
    public List<BasicSettingsVo> listBySourceUser(List<String> sourceIds, Integer sourceType, Long userId, Long tenantId) {
        if (ObjectUtil.isEmpty(sourceIds)) {
            return List.of();
        }
        List<BasicSettingsEntity> list = basicSettingsService.lambdaQuery()
                .in(BasicSettingsEntity::getSourceId, sourceIds)
                .eq(BasicSettingsEntity::getSourceType, sourceType)
                .eq(BasicSettingsEntity::getUserId, userId)
                .eq(BasicSettingsEntity::getTenantId, tenantId)
                .list();

        if (ObjectUtil.isNotEmpty(list)) {
            return BeanUtil.copyToList(list, BasicSettingsVo.class);
        }
        return List.of();
    }

    @Override
    public List<BasicSettingsVo> listBySourceUser(List<String> sourceIds, Integer sourceType, List<Long> userIds, Long tenantId) {
        if (ObjectUtil.isEmpty(sourceIds)) {
            return List.of();
        }
        List<BasicSettingsEntity> list = basicSettingsService.lambdaQuery()
                .in(BasicSettingsEntity::getSourceId, sourceIds)
                .eq(BasicSettingsEntity::getSourceType, sourceType)
                .in(BasicSettingsEntity::getUserId, userIds)
                .eq(BasicSettingsEntity::getTenantId, tenantId)
                .list();

        if (ObjectUtil.isNotEmpty(list)) {
            return BeanUtil.copyToList(list, BasicSettingsVo.class);
        }
        return List.of();
    }
}