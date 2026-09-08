package com.jiuyu.replay.common.repository.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.jiuyu.replay.common.entity.MqMessageRecordEntity;

import java.util.List;

/**
 * <p>
 * MQ消息记录表 服务类
 * </p>
 *
 * @author RayChou
 * @since 2025-06-03
 */
public interface MqMessageRecordService extends IService<MqMessageRecordEntity> {

    /**
     * 查询消息列表根据消息状态
     *
     * @param status 消息状态：0：待发送 1：已发送 2：已消费 3：发送失败 4：消费失败
     * @param limit  查询限制条数
     * @return
     */
    List<MqMessageRecordEntity> listMessageRecordByStatus(int status, int limit);

    /**
     * 查询待发送的消息（包括立即发送和延时发送）
     *
     * @param limit 查询限制条数
     * @return
     */
    List<MqMessageRecordEntity> listPendingMessages(int limit);
}
