package com.jiuyu.replay.api.logic.order.impl;

import cn.hutool.core.util.ObjectUtil;
import com.jiuyu.replay.api.logic.order.InvitationCodeBatchLogic;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.order.bll.InvitationCodeBatchBll;
import com.jiuyu.replay.order.bll.TypeConsumptionBll;
import com.jiuyu.replay.order.bo.InvitationCodeBatchBo;
import com.jiuyu.replay.order.bo.InvitationCodeBatchListBo;
import com.jiuyu.replay.order.vo.*;
import com.jiuyu.replay.power.bll.UserBll;
import com.jiuyu.replay.power.utils.GlobalObject;
import com.jiuyu.replay.power.vo.UserCacheVo;
import com.jiuyu.replay.power.vo.UserListVo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;


/**
 * 邀请码-批次
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-10-15 15:57:30
 */
@Service
public class InvitationCodeBatchLogicImpl implements InvitationCodeBatchLogic {

    @Resource
    private InvitationCodeBatchBll invitationCodeBatchBll;
    @Resource
    private UserBll userBll;
    @Resource
    private TypeConsumptionBll typeConsumptionBll;


    @Override
    public R<PageUtils<InvitationCodeBatchListVo>> queryPage(InvitationCodeBatchListBo invitationCodeBatchListBo) {

        return invitationCodeBatchBll.queryPage(invitationCodeBatchListBo);
    }

    @Override
    public R<InvitationCodeBatchInfoVo> info(Long id) {

        R<InvitationCodeBatchInfoVo> batchInfoVoR = invitationCodeBatchBll.info(id);
        InvitationCodeBatchInfoVo data = batchInfoVoR.getData();
        if(data != null) {
            // 封装邀请码使用人的名字
            List<InvitationCodeInfoVo> codeList = data.getCodeList();
            if(codeList != null && codeList.size() > 0) {
                List<Long> userIds = codeList.stream().map(InvitationCodeVo::getUserId).filter(userId -> !userId.equals(0L)).toList();
                if(userIds.size() > 0) {
                    R<List<UserListVo>> listR = this.userBll.listByIds(userIds);
                    List<UserListVo> userListVoList = listR.getData();
                    if(userListVoList != null && userListVoList.size() > 0) {
                        for (InvitationCodeInfoVo invitationCodeInfoVo : codeList) {
                            if(!invitationCodeInfoVo.getUserId().equals(0L)) {
                                for (UserListVo userListVo : userListVoList) {
                                    if(invitationCodeInfoVo.getUserId().equals(userListVo.getId())) {
                                        invitationCodeInfoVo.setUserName(userListVo.getNickName());
                                        invitationCodeInfoVo.setUserPhone(userListVo.getPhone());
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        return batchInfoVoR;
    }

    @Override
    public R<String> save(InvitationCodeBatchBo invitationCodeBatchBo) {

        UserCacheVo user = GlobalObject.getLocalUser();
        invitationCodeBatchBo.setUserId(user.getId());

        return invitationCodeBatchBll.save(invitationCodeBatchBo);
    }

    @Override
    public R<String> update(InvitationCodeBatchBo invitationCodeBatchBo) {

        return invitationCodeBatchBll.update(invitationCodeBatchBo);
    }

    @Override
    public R<String> delete(Long id) {

        return invitationCodeBatchBll.delete(id);
    }

    /**
     * 根据邀请码批次导出该批次的所有邀请码
     * @param batchId
     * @return
     */
    @Override
    public R<List<InvitationCodeListVo>> invitationByBatchId(Long batchId) {
        R<List<InvitationCodeListVo>> listR = invitationCodeBatchBll.invitationByBatchId(batchId);
        // 封装邀请码使用人的名字
        List<InvitationCodeListVo> data = listR.getData();
        if(data != null && !data.isEmpty()) {
            List<Long> userIds = data.stream().map(InvitationCodeVo::getUserId).filter(userId -> !userId.equals(0L)).toList();
            if(!userIds.isEmpty()) {
                R<List<UserListVo>> listR1 = this.userBll.listByIds(userIds);
                List<UserListVo> userListVoList = listR1.getData();
                if(userListVoList != null && userListVoList.size() > 0) {
                    for (InvitationCodeListVo invitationCodeListVo : data) {
                        if(!invitationCodeListVo.getUserId().equals(0L)) {
                            for (UserListVo userListVo : userListVoList) {
                                if(invitationCodeListVo.getUserId().equals(userListVo.getId())) {
                                    invitationCodeListVo.setUserName(userListVo.getNickName());
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        }
        return listR;
    }

    /**
     * 只查询激活码信息
     * @return
     */
    @Override
    public R<InvitationCodeBatchInfoVo> checkOnlyActivationCode() {
        InvitationCodeBatchListBo bo = new InvitationCodeBatchListBo();
        bo.setLimit(-1);
        bo.setType(2);
        R<PageUtils<InvitationCodeBatchListVo>> pageUtilsR = invitationCodeBatchBll.queryPage(bo);
        if (ObjectUtil.isNotEmpty(pageUtilsR.getData()) && ObjectUtil.isNotEmpty(pageUtilsR.getData().getList())){
            List<InvitationCodeBatchListVo> list = pageUtilsR.getData().getList();
            return invitationCodeBatchBll.info(list.get(0).getId());
        }
        return R.error(3001, "当前没有激活码");
    }

    @Override
    public R<List<TypeConsumptionInfoVo>> getTypeConsumptionById(Long id) {
        return typeConsumptionBll.listBySourceId(id, 2);
    }
}

