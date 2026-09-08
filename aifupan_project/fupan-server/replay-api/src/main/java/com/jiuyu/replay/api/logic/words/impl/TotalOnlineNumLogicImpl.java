package com.jiuyu.replay.api.logic.words.impl;

import com.jiuyu.replay.api.logic.words.TotalOnlineNumLogic;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.power.utils.GlobalObject;
import com.jiuyu.replay.power.vo.UserCacheVo;
import com.jiuyu.replay.words.bll.TotalOnlineNumBll;
import com.jiuyu.replay.words.bo.TotalOnlineNumBo;
import com.jiuyu.replay.words.bo.TotalOnlineNumListBo;
import com.jiuyu.replay.words.vo.TotalOnlineNumInfoVo;
import com.jiuyu.replay.words.vo.TotalOnlineNumListVo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;


/**
 * 直播总观看人次
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-11-14 10:05:41
 */
@Service
public class TotalOnlineNumLogicImpl implements TotalOnlineNumLogic {

    @Resource
    private TotalOnlineNumBll totalOnlineNumBll;


    @Override
    public R<PageUtils<TotalOnlineNumListVo>> queryPage(TotalOnlineNumListBo totalOnlineNumListBo) {

        return totalOnlineNumBll.queryPage(totalOnlineNumListBo);
    }

    @Override
    public R<TotalOnlineNumInfoVo> info(Long id) {

        return totalOnlineNumBll.info(id);
    }

    @Override
    public R<String> save(TotalOnlineNumBo totalOnlineNumBo) {

        UserCacheVo user = GlobalObject.getLocalUser();
        totalOnlineNumBo.setUserId(user.getId());
        totalOnlineNumBo.setTenantId(user.getActiveTenantId());

        return totalOnlineNumBll.save(totalOnlineNumBo);
    }

    @Override
    public R<String> update(TotalOnlineNumBo totalOnlineNumBo) {

        UserCacheVo user = GlobalObject.getLocalUser();
        totalOnlineNumBo.setUserId(user.getId());

        return totalOnlineNumBll.update(totalOnlineNumBo);
    }

    @Override
    public R<String> delete(Long id) {

        return totalOnlineNumBll.delete(id);
    }

    @Override
    public R<String> saveOrUpdate(TotalOnlineNumBo totalOnlineNumBo) {

        UserCacheVo user = GlobalObject.getLocalUser();
        totalOnlineNumBo.setUserId(user.getId());
        totalOnlineNumBo.setTenantId(user.getActiveTenantId());

        return totalOnlineNumBll.saveOrUpdate(totalOnlineNumBo);
    }


}

