package com.jiuyu.replay.power.repository.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jiuyu.replay.common.utils.SnowflakeManager;
import com.jiuyu.replay.power.bo.crm.CrmBusinessSnapshotUpdateBo;
import com.jiuyu.replay.power.bo.crm.CrmStageEventSaveBo;
import com.jiuyu.replay.power.entity.CrmStageEventEntity;
import com.jiuyu.replay.power.entity.UserBusinessEntity;
import com.jiuyu.replay.power.mapper.CrmStageEventMapper;
import com.jiuyu.replay.power.repository.service.CrmStageEventService;
import com.jiuyu.replay.power.repository.service.UserBusinessService;
import com.jiuyu.replay.power.vo.crm.CrmStageEventInfoVo;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service("crmStageEventService")
@Slf4j
@AllArgsConstructor
public class CrmStageEventServiceImpl extends ServiceImpl<CrmStageEventMapper, CrmStageEventEntity>
        implements CrmStageEventService {

    private static final String LOG_PREFIX = "[CRM-STAGE-EVENT]";

    private final UserBusinessService userBusinessService;

    /**
     * 按 eventId 幂等写入阶段事件
     *
     * @param bo 阶段事件保存参数
     *
     * @return {@link CrmStageEventInfoVo }
     */
    @Override
    public CrmStageEventInfoVo saveOrUpdateByEventId(CrmStageEventSaveBo bo) {
        if (bo == null || bo.getEventId() == null) {
            return null;
        }

        CrmStageEventEntity entity = this.lambdaQuery()
                .eq(CrmStageEventEntity::getEventId, bo.getEventId())
                .last("limit 1")
                .one();

        if (entity == null) {
            entity = new CrmStageEventEntity();
            entity.setId(SnowflakeManager.nextValue());
            entity.setCreateDate(new Date());
        }

        entity.setUserId(bo.getUserId());
        entity.setEventId(bo.getEventId());
        entity.setSource(bo.getSource());
        entity.setOccurredAt(bo.getOccurredAt());
        entity.setStageCode(bo.getStageCode());
        entity.setStageLabel(bo.getStageLabel());
        entity.setConfidence(bo.getConfidence());
        entity.setSummary(bo.getSummary());
        entity.setFactsJson(bo.getFactsJson());
        entity.setRawJson(bo.getRawJson());
        this.saveOrUpdate(entity);

        log.info("{} saveOrUpdate success, userId={}, eventId={}", LOG_PREFIX, entity.getUserId(), entity.getEventId());
        return BeanUtil.copyProperties(entity, CrmStageEventInfoVo.class);
    }

    /**
     * 受控刷新客户销售快照
     *
     * @param bo 快照刷新参数
     */
    @Override
    public void updateBusinessSnapshot(CrmBusinessSnapshotUpdateBo bo) {
        if (bo == null || bo.getUserId() == null) {
            return;
        }

        UserBusinessEntity business = userBusinessService.getById(bo.getUserId());
        if (business == null) {
            business = new UserBusinessEntity();
            business.setCreateDate(new Date());
        }
        business.setUserId(bo.getUserId());
        business.setAccordingStatus(bo.getAccordingStatus());
        business.setAccordingContent(bo.getAccordingContent());
        business.setAccordingDate(bo.getAccordingDate());
        business.setUpdateDate(new Date());
        userBusinessService.saveOrUpdate(business);
    }
}
