package com.jiuyu.replay.common.bll;

import com.jiuyu.replay.common.bo.ComputerConfigBo;
import com.jiuyu.replay.common.entity.ComputerConfigEntity;
import com.jiuyu.replay.common.repository.service.ComputerConfigService;
import com.jiuyu.replay.common.utils.SnowflakeManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

@Slf4j
@Component
@RequiredArgsConstructor
public class ComputerConfigBll {

    private final ComputerConfigService computerConfigService;

    @Transactional(rollbackFor = Exception.class)
    public void saveOrUpdate(ComputerConfigBo bo, Long userId) {
        ComputerConfigEntity exist = computerConfigService.lambdaQuery()
                .eq(ComputerConfigEntity::getCpuId, bo.getCpuId())
                .one();

        Date now = new Date();
        ComputerConfigEntity entity;
        if (exist != null) {
            entity = exist;
        } else {
            entity = new ComputerConfigEntity();
            entity.setId(SnowflakeManager.nextValue());
            entity.setCreateDate(now);
            entity.setCpuId(bo.getCpuId());
        }
        entity.setUserId(userId);
        BeanUtils.copyProperties(bo, entity, "id", "cpuId", "userId", "createDate");
        entity.setUpdateDate(now);
        computerConfigService.saveOrUpdate(entity);
    }
}
