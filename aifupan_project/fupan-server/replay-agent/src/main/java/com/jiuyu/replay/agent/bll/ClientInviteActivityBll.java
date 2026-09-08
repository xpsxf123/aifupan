package com.jiuyu.replay.agent.bll;

import com.jiuyu.replay.agent.bo.ClientInviteActivityBo;
import com.jiuyu.replay.agent.bo.ClientInviteActivityListBo;
import com.jiuyu.replay.agent.bo.ClientInviteProgressBo;
import com.jiuyu.replay.agent.producer.ClientInviteActivityProducer;
import com.jiuyu.replay.agent.producer.ClientInviteProgressProducer;
import com.jiuyu.replay.agent.vo.ClientInviteActivityInfoVo;
import com.jiuyu.replay.agent.vo.ClientInviteActivityListVo;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import jakarta.annotation.Resource;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


/**
 * 邀请活动
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-05-23 16:45:49
 */
//@Component
public class ClientInviteActivityBll {

    @Resource
    private ClientInviteActivityProducer clientInviteActivityProducer;
    @Resource
    private ClientInviteProgressProducer clientInviteProgressProducer;


    /**
     * 邀请活动列表
     * @param clientInviteActivityListBo 邀请活动列表查询参数
     * @return
     */
    public R<PageUtils<ClientInviteActivityListVo>> queryPage(ClientInviteActivityListBo clientInviteActivityListBo) {

        return R.ok("获取成功", clientInviteActivityProducer.queryPage(clientInviteActivityListBo));
    }

    /**
    * 邀请活动信息
    * @param id 邀请活动id
     * @param activityStatus 活动状态 0：未启用 1：启用中
    * @return
    */
    public R<ClientInviteActivityInfoVo> info(Long id, Integer activityStatus) {

        ClientInviteActivityInfoVo clientInviteActivityInfoVo = clientInviteActivityProducer.info(id, activityStatus);
        return R.ok("获取成功", clientInviteActivityInfoVo);
    }

    /**
     * 新增邀请活动
     * @param clientInviteActivityBo 邀请活动对象
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    public R<String> save(ClientInviteActivityBo clientInviteActivityBo) {

        // 保存活动信息
        ClientInviteActivityInfoVo clientInviteActivityInfoVo = clientInviteActivityProducer.save(clientInviteActivityBo);
        // 保存进度和奖励信息
        List<ClientInviteProgressBo> progressBoList = clientInviteActivityBo.getClientInviteProgressBoList();
        if(progressBoList != null && progressBoList.size() > 0) {
            for (ClientInviteProgressBo clientInviteProgressBo : progressBoList) {
                clientInviteProgressBo.setInviteActivityId(clientInviteActivityInfoVo.getId());
            }
            clientInviteProgressProducer.saveBatch(clientInviteActivityBo.getClientInviteProgressBoList());
        }

        return R.ok("添加成功");
    }

    /**
     * 修改邀请活动
     * @param clientInviteActivityBo 邀请活动对象
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    public R<String> update(ClientInviteActivityBo clientInviteActivityBo) {
        // 修改活动信息
        clientInviteActivityProducer.update(clientInviteActivityBo);
        // 将旧的所有进度状态改为停用
        clientInviteProgressProducer.disableOld(clientInviteActivityBo.getId());
        // 保存进度和奖励信息
        List<ClientInviteProgressBo> progressBoList = clientInviteActivityBo.getClientInviteProgressBoList();
        if(progressBoList != null && progressBoList.size() > 0) {
            for (ClientInviteProgressBo clientInviteProgressBo : progressBoList) {
                clientInviteProgressBo.setInviteActivityId(clientInviteActivityBo.getId());
            }
            clientInviteProgressProducer.saveBatch(clientInviteActivityBo.getClientInviteProgressBoList());
        }

        return R.ok("修改成功");
    }

    /**
     * 删除邀请活动
     * @param id 邀请活动id
     * @return
     */
    public R<String> delete(Long id) {

        clientInviteActivityProducer.deleteById(id);
        return R.ok("删除成功");
    }


}

