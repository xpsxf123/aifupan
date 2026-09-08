package com.jiuyu.replay.power.repository.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.jiuyu.replay.power.bo.crm.CrmAiProfileBo;
import com.jiuyu.replay.power.entity.CrmAiProfileEntity;
import com.jiuyu.replay.power.vo.crm.CrmAiProfileInfoVo;

/**
 * CRM AI画像表
 */
public interface CrmAiProfileService extends IService<CrmAiProfileEntity> {

    /**
     * 按 profileId 幂等写入 AI 画像
     *
     * @param bo AI 画像保存参数
     *
     * @return {@link CrmAiProfileInfoVo }
     */
    CrmAiProfileInfoVo saveOrUpdateByProfileId(CrmAiProfileBo bo);
}
