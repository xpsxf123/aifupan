package com.jiuyu.replay.power.repository.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.jiuyu.replay.power.bo.crm.CrmBusinessSnapshotUpdateBo;
import com.jiuyu.replay.power.bo.crm.CrmStageEventSaveBo;
import com.jiuyu.replay.power.entity.CrmStageEventEntity;
import com.jiuyu.replay.power.vo.crm.CrmStageEventInfoVo;

/**
 * CRM 阶段事件表
 */
public interface CrmStageEventService extends IService<CrmStageEventEntity> {

    /**
     * 按 eventId 幂等写入阶段事件
     *
     * @param bo 阶段事件保存参数
     *
     * @return {@link CrmStageEventInfoVo }
     */
    CrmStageEventInfoVo saveOrUpdateByEventId(CrmStageEventSaveBo bo);

    /**
     * 受控刷新客户销售快照
     *
     * @param bo 快照刷新参数
     */
    void updateBusinessSnapshot(CrmBusinessSnapshotUpdateBo bo);
}
