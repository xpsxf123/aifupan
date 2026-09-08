package com.jiuyu.replay.power.repository.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jiuyu.replay.common.utils.SnowflakeManager;
import com.jiuyu.replay.power.bo.crm.CrmAiProfileBo;
import com.jiuyu.replay.power.entity.CrmAiProfileEntity;
import com.jiuyu.replay.power.mapper.CrmAiProfileMapper;
import com.jiuyu.replay.power.repository.service.CrmAiProfileService;
import com.jiuyu.replay.power.vo.crm.CrmAiProfileInfoVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service("crmAiProfileService")
@Slf4j
public class CrmAiProfileServiceImpl extends ServiceImpl<CrmAiProfileMapper, CrmAiProfileEntity>
        implements CrmAiProfileService {

    private static final String LOG_PREFIX = "[CRM-AI-PROFILE]";

    /**
     * 按 profileId 幂等写入 AI 画像
     *
     * @param bo AI 画像保存参数
     *
     * @return {@link CrmAiProfileInfoVo }
     */
    @Override
    public CrmAiProfileInfoVo saveOrUpdateByProfileId(CrmAiProfileBo bo) {
        if (bo == null || bo.getProfileId() == null) {
            return null;
        }

        CrmAiProfileEntity entity = this.lambdaQuery()
                .eq(CrmAiProfileEntity::getProfileId, bo.getProfileId())
                .last("limit 1")
                .one();

        Date now = new Date();
        if (entity == null) {
            entity = new CrmAiProfileEntity();
            entity.setId(SnowflakeManager.nextValue());
            entity.setCreateDate(now);
            entity.setIsDeleted(0);
        }

        entity.setUserId(bo.getUserId());
        entity.setProfileId(bo.getProfileId());
        entity.setSource(bo.getSource());
        entity.setUpdatedAt(bo.getUpdatedAt());
        entity.setProfileJson(bo.getProfileJson());
        entity.setSummary(bo.getSummary());
        entity.setUpdateDate(now);
        this.saveOrUpdate(entity);

        log.info("{} saveOrUpdate success, userId={}, profileId={}", LOG_PREFIX, entity.getUserId(), entity.getProfileId());
        return BeanUtil.copyProperties(entity, CrmAiProfileInfoVo.class);
    }
}
