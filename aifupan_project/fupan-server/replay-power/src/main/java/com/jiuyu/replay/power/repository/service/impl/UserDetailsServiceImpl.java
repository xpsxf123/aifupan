package com.jiuyu.replay.power.repository.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jiuyu.replay.common.utils.SnowflakeManager;
import com.jiuyu.replay.power.bo.crm.CrmUserDetailsSyncBo;
import com.jiuyu.replay.power.entity.UserDetailsEntity;
import com.jiuyu.replay.power.repository.dao.UserDetailsDao;
import com.jiuyu.replay.power.repository.service.UserDetailsService;
import com.jiuyu.replay.power.vo.crm.CrmUserDetailsSyncVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Date;


@Service("userDetailsService")
@Slf4j
public class UserDetailsServiceImpl extends ServiceImpl<UserDetailsDao, UserDetailsEntity> implements UserDetailsService {

    private static final String LOG_PREFIX = "[CRM-USER-DETAILS]";

    @Override
    public Long countClientDetailBySaleId(Long salesId) {
        return baseMapper.countClientDetailBySaleId(salesId);
    }

    /**
     * 保存 CRM 结构化画像字段
     *
     * @param bo CRM 画像字段同步对象
     *
     * @return 同步结果
     */
    @Override
    public CrmUserDetailsSyncVo saveCrmProfileFields(CrmUserDetailsSyncBo bo) {
        if (bo == null || bo.getUserId() == null) {
            log.warn("{} saveCrmProfileFields ignored, userId empty", LOG_PREFIX);
            return null;
        }

        UserDetailsEntity entity = this.lambdaQuery()
                .eq(UserDetailsEntity::getUserId, bo.getUserId())
                .last("limit 1")
                .one();

        Date now = new Date();
        if (entity == null) {
            entity = new UserDetailsEntity();
            entity.setId(SnowflakeManager.nextValue());
            entity.setUserId(bo.getUserId());
            entity.setCreateDate(now);
            entity.setIsDeleted(0);
        }

        entity.setUserAmbition(bo.getUserAmbition());
        entity.setUserBelongType(bo.getUserBelongType());
        entity.setUpdateDate(now);
        this.saveOrUpdate(entity);

        CrmUserDetailsSyncVo result = new CrmUserDetailsSyncVo();
        result.setUserId(entity.getUserId());
        result.setUserAmbition(entity.getUserAmbition());
        result.setUserBelongType(entity.getUserBelongType());
        log.info("{} saveCrmProfileFields success, userId={}", LOG_PREFIX, entity.getUserId());
        return result;
    }
}
