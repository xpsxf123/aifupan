package com.jiuyu.replay.api.service.crm;

import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.power.bo.crm.CrmAiProfileUpsertBo;
import com.jiuyu.replay.power.bo.crm.CrmBatchAggregateQueryBo;
import com.jiuyu.replay.power.bo.crm.CrmBehaviorSignalsQueryBo;
import com.jiuyu.replay.power.bo.crm.CrmProfileSyncBo;
import com.jiuyu.replay.power.bo.crm.CrmStageEventBo;
import com.jiuyu.replay.power.vo.crm.CrmAiProfileUpsertVo;
import com.jiuyu.replay.power.vo.crm.CrmBatchAggregateQueryVo;
import com.jiuyu.replay.power.vo.crm.CrmBehaviorSignalsVo;
import com.jiuyu.replay.power.vo.crm.CrmOrderQueryByPhoneVo;
import com.jiuyu.replay.power.vo.crm.CrmProfileSyncVo;
import com.jiuyu.replay.power.vo.crm.CrmStageEventVo;

/**
 * CRM 集成服务
 * 统一承载对销售智能体开放的 CRM 查询与回流写入能力。
 */
public interface CrmIntegrationService {

    /**
     * 批量聚合查询客户信息
     *
     * @param bo 查询条件
     *
     * @return {@link R }<{@link CrmBatchAggregateQueryVo }>
     */
    R<CrmBatchAggregateQueryVo> batchAggregateQuery(CrmBatchAggregateQueryBo bo);

    /**
     * 按手机号查询订单
     *
     * @param phone 客户手机号
     *
     * @return {@link R }<{@link CrmOrderQueryByPhoneVo }>
     */
    R<CrmOrderQueryByPhoneVo> queryOrderByPhone(String phone);

    /**
     * 同步客户画像字段
     *
     * @param bo 画像字段同步请求
     *
     * @return {@link R }<{@link CrmProfileSyncVo }>
     */
    R<CrmProfileSyncVo> profileSync(CrmProfileSyncBo bo);

    /**
     * 写入 AI 画像
     *
     * @param bo AI 画像写入请求
     *
     * @return {@link R }<{@link CrmAiProfileUpsertVo }>
     */
    R<CrmAiProfileUpsertVo> upsertAiProfile(CrmAiProfileUpsertBo bo);

    /**
     * 回传客户阶段事件
     *
     * @param bo 阶段事件回传请求
     *
     * @return {@link R }<{@link CrmStageEventVo }>
     */
    R<CrmStageEventVo> saveStageEvent(CrmStageEventBo bo);

    /**
     * 批量查询客户行为信号（试用期使用深度）
     *
     * @param bo 查询条件，包含手机号列表
     *
     * @return {@link R }<{@link CrmBehaviorSignalsVo }>
     */
    R<CrmBehaviorSignalsVo> behaviorSignals(CrmBehaviorSignalsQueryBo bo);
}

