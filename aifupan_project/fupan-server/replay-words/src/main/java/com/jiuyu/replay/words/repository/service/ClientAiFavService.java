package com.jiuyu.replay.words.repository.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.jiuyu.replay.words.bo.ClientAiFavBo;
import com.jiuyu.replay.words.entity.ClientAiFavEntity;

/**
 * 运营/违规收藏列表
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-03-12 16:42:18
 */
public interface ClientAiFavService extends IService<ClientAiFavEntity> {

    /**
     * 判断是否存在已删除的数据
     * @param clientAiFavBo
     * @return
     */
    int hasDelete(ClientAiFavBo clientAiFavBo);
}

