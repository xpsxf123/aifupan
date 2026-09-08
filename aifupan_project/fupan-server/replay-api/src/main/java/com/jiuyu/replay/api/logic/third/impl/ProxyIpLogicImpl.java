package com.jiuyu.replay.api.logic.third.impl;

import com.jiuyu.replay.api.logic.third.ProxyIpLogic;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.power.utils.GlobalObject;
import com.jiuyu.replay.power.vo.UserCacheVo;
import com.jiuyu.replay.third.bll.ProxyIpBll;
import com.jiuyu.replay.third.bo.ProxyIpBo;
import com.jiuyu.replay.third.bo.ProxyIpListBo;
import com.jiuyu.replay.third.vo.ProxyIpInfoVo;
import com.jiuyu.replay.third.vo.ProxyIpListVo;
import com.jiuyu.replay.third.vo.ProxyIpRecordInfoVo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;


/**
 * 代理ip提取
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-02-27 10:37:17
 */
@Service
public class ProxyIpLogicImpl implements ProxyIpLogic {

    @Resource
    private ProxyIpBll proxyIpBll;


    @Override
    public R<PageUtils<ProxyIpListVo>> queryPage(ProxyIpListBo proxyIpListBo) {

        return proxyIpBll.queryPage(proxyIpListBo);
    }

    @Override
    public R<ProxyIpInfoVo> info(Long id) {

        return proxyIpBll.info(id);
    }

    @Override
    public R<String> save(ProxyIpBo proxyIpBo) {

        return proxyIpBll.save(proxyIpBo);
    }

    @Override
    public R<String> update(ProxyIpBo proxyIpBo) {

        return proxyIpBll.update(proxyIpBo);
    }

    @Override
    public R<String> delete(Long id) {

        return proxyIpBll.delete(id);
    }

    @Override
    public R<ProxyIpRecordInfoVo> getProxyIp(Boolean forceUpdate, Integer validityType) {

        UserCacheVo user = GlobalObject.getLocalUser();

        return proxyIpBll.getProxyIp(user.getId(), user.getActiveTenantId(), forceUpdate, validityType);
    }


}

