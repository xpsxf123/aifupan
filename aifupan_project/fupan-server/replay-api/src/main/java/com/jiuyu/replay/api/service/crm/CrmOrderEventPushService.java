package com.jiuyu.replay.api.service.crm;

import com.jiuyu.replay.api.bo.crm.CrmOrderEventPushBo;
import com.jiuyu.replay.api.vo.crm.CrmOrderEventPushResultVo;

/**
 * CRM 订单事件出站推送服务
 * 负责将订单事件推送到智能体侧接口。
 */
public interface CrmOrderEventPushService {

    /**
     * 推送订单事件到智能体侧接口
     *
     * @param bo 订单事件请求体
     *
     * @return 推送结果
     *
     * @throws Exception HTTP 调用异常
     */
    CrmOrderEventPushResultVo pushOrderEvent(CrmOrderEventPushBo bo) throws Exception;
}

