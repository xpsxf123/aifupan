package com.jiuyu.replay.common.repository.service.impl;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jiuyu.replay.common.entity.MqMessageRecordEntity;
import com.jiuyu.replay.common.repository.dao.MqMessageRecordDao;
import com.jiuyu.replay.common.repository.service.MqMessageRecordService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * <p>
 * MQ消息记录表 服务实现类
 * </p>
 *
 * @author RayChou
 * @since 2025-06-03
 */
@Service
public class MqMessageRecordServiceImpl extends ServiceImpl<MqMessageRecordDao, MqMessageRecordEntity> implements MqMessageRecordService {

    @Override
    public List<MqMessageRecordEntity> listMessageRecordByStatus(int status, int limit) {
        return list(new LambdaQueryWrapper<>(MqMessageRecordEntity.class).eq(MqMessageRecordEntity::getMessageStatus, status).orderByAsc(MqMessageRecordEntity::getCreateDate).last("limit " + limit));
    }

    @Override
    public List<MqMessageRecordEntity> listPendingMessages(int limit) {
        return list(new LambdaQueryWrapper<>(MqMessageRecordEntity.class)
                .eq(MqMessageRecordEntity::getMessageStatus, 0) // 待发送状态
                .orderByAsc(MqMessageRecordEntity::getCreateDate)
                .last("limit " + limit));
    }
}
