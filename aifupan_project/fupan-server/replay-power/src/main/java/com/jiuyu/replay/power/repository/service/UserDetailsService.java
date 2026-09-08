package com.jiuyu.replay.power.repository.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.jiuyu.replay.power.bo.crm.CrmUserDetailsSyncBo;
import com.jiuyu.replay.power.entity.UserDetailsEntity;
import com.jiuyu.replay.power.vo.crm.CrmUserDetailsSyncVo;

/**
 * 用户详情表
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-09-14 10:12:41
 */
public interface UserDetailsService extends IService<UserDetailsEntity> {

    Long countClientDetailBySaleId(Long salesId);

    /**
     * 保存 CRM 结构化画像字段
     *
     * @param bo CRM 画像字段同步对象
     *
     * @return 同步结果
     */
    CrmUserDetailsSyncVo saveCrmProfileFields(CrmUserDetailsSyncBo bo);
}
