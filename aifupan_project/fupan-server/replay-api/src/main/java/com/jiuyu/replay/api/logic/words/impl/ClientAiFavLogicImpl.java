package com.jiuyu.replay.api.logic.words.impl;

import com.jiuyu.replay.api.logic.words.ClientAiFavLogic;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.power.utils.GlobalObject;
import com.jiuyu.replay.power.vo.UserCacheVo;
import com.jiuyu.replay.words.bll.ClientAiFavBll;
import com.jiuyu.replay.words.bo.ClientAiFavBo;
import com.jiuyu.replay.words.bo.ClientAiFavListBo;
import com.jiuyu.replay.words.vo.ClientAiFavInfoVo;
import com.jiuyu.replay.words.vo.ClientAiFavListVo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;


/**
 * 运营/违规收藏列表
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-03-12 16:42:18
 */
@Service
public class ClientAiFavLogicImpl implements ClientAiFavLogic {

    @Resource
    private ClientAiFavBll clientAiFavBll;


    @Override
    public R<PageUtils<ClientAiFavListVo>> queryPage(ClientAiFavListBo clientAiFavListBo) {

        UserCacheVo user = GlobalObject.getLocalUser();
        clientAiFavListBo.setUserId(user.getId());
        clientAiFavListBo.setTenantId(user.getActiveTenantId());

        return clientAiFavBll.queryPage(clientAiFavListBo);
    }

    @Override
    public R<ClientAiFavInfoVo> info(Long id) {

        return clientAiFavBll.info(id);
    }

    @Override
    public R<String> save(ClientAiFavBo clientAiFavBo) {

        return clientAiFavBll.save(clientAiFavBo);
    }

    @Override
    public R<String> update(ClientAiFavBo clientAiFavBo) {

        return clientAiFavBll.update(clientAiFavBo);
    }

    @Override
    public R<String> delete(Long id) {

        return clientAiFavBll.delete(id);
    }

    @Override
    public R<String> batchDelete(List<String> ids) {

        UserCacheVo user = GlobalObject.getLocalUser();

        return clientAiFavBll.batchDelete(ids, user.getId(), user.getActiveTenantId());
    }

    @Override
    public R<String> saveOrUpdate(ClientAiFavBo clientAiFavBo) {
        UserCacheVo user = GlobalObject.getLocalUser();
        clientAiFavBo.setUserId(user.getId());
        clientAiFavBo.setTenantId(user.getActiveTenantId());
        return clientAiFavBll.saveOrUpdate(clientAiFavBo);
    }

    @Override
    public R<String> saveOrUpdateByNotExist(ClientAiFavBo clientAiFavBo) {
        UserCacheVo user = GlobalObject.getLocalUser();
        clientAiFavBo.setUserId(user.getId());
        clientAiFavBo.setTenantId(user.getActiveTenantId());
        return clientAiFavBll.saveOrUpdateByNotExist(clientAiFavBo);
    }
}

