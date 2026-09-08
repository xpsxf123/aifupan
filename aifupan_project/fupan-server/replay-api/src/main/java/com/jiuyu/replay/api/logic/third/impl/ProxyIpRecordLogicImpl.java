package com.jiuyu.replay.api.logic.third.impl;

import com.jiuyu.replay.api.logic.third.ProxyIpRecordLogic;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.power.bll.UserBll;
import com.jiuyu.replay.third.bll.ProxyIpRecordBll;
import com.jiuyu.replay.third.bo.ProxyIpRecordBo;
import com.jiuyu.replay.third.bo.ProxyIpRecordListBo;
import com.jiuyu.replay.third.vo.ProxyIpRecordInfoVo;
import com.jiuyu.replay.third.vo.ProxyIpRecordListVo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;


/**
 * 代理ip提取记录
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-02-27 10:37:17
 */
@Service
public class ProxyIpRecordLogicImpl implements ProxyIpRecordLogic {

    @Resource
    private ProxyIpRecordBll proxyIpRecordBll;
    @Resource
    private UserBll userBll;


    @Override
    public R<PageUtils<ProxyIpRecordListVo>> queryPage(ProxyIpRecordListBo proxyIpRecordListBo) {

        if(!StringUtils.isEmpty(proxyIpRecordListBo.getKeyword())) {
            R<List<Long>> userIdsR = userBll.listIdsByLikePhoneOrName(proxyIpRecordListBo.getKeyword());
            if(userIdsR.getCode() == 0) {
                List<Long> userIds = userIdsR.getData();
                if(userIds != null && userIds.size() > 0) {
                    proxyIpRecordListBo.setUserIds(userIds);
                }
            }
        }

        return proxyIpRecordBll.queryPage(proxyIpRecordListBo);
    }

    @Override
    public R<ProxyIpRecordInfoVo> info(Long id) {

        return proxyIpRecordBll.info(id);
    }

    @Override
    public R<String> save(ProxyIpRecordBo proxyIpRecordBo) {

        return proxyIpRecordBll.save(proxyIpRecordBo);
    }

    @Override
    public R<String> update(ProxyIpRecordBo proxyIpRecordBo) {

        return proxyIpRecordBll.update(proxyIpRecordBo);
    }

    @Override
    public R<String> delete(Long id) {

        return proxyIpRecordBll.delete(id);
    }


}

