package com.jiuyu.replay.agent.bll;

import com.jiuyu.replay.agent.bo.ClientInviteProgressRewardBo;
import com.jiuyu.replay.agent.bo.ClientInviteProgressRewardListBo;
import com.jiuyu.replay.agent.producer.ClientInviteProgressRewardProducer;
import com.jiuyu.replay.agent.vo.ClientInviteProgressRewardInfoVo;
import com.jiuyu.replay.agent.vo.ClientInviteProgressRewardListVo;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import jakarta.annotation.Resource;


/**
 * 邀请进度奖励
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-05-20 10:36:55
 */
//@Component
public class ClientInviteProgressRewardBll {

    @Resource
    private ClientInviteProgressRewardProducer clientInviteProgressRewardProducer;


    /**
     * 邀请进度奖励列表
     * @param clientInviteProgressRewardListBo 邀请进度奖励列表查询参数
     * @return
     */
    public R<PageUtils<ClientInviteProgressRewardListVo>> queryPage(ClientInviteProgressRewardListBo clientInviteProgressRewardListBo) {

        return R.ok("获取成功", clientInviteProgressRewardProducer.queryPage(clientInviteProgressRewardListBo));
    }

    /**
    * 邀请进度奖励信息
    * @param id 邀请进度奖励id
    * @return
    */
    public R<ClientInviteProgressRewardInfoVo> info(Long id) {

        ClientInviteProgressRewardInfoVo clientInviteProgressRewardInfoVo = clientInviteProgressRewardProducer.info(id);
        return R.ok("获取成功", clientInviteProgressRewardInfoVo);
    }

    /**
     * 新增邀请进度奖励
     * @param clientInviteProgressRewardBo 邀请进度奖励对象
     * @return
     */
    public R<String> save(ClientInviteProgressRewardBo clientInviteProgressRewardBo) {

        ClientInviteProgressRewardInfoVo clientInviteProgressRewardInfoVo = clientInviteProgressRewardProducer.save(clientInviteProgressRewardBo);
        return R.ok("添加成功");
    }

    /**
     * 修改邀请进度奖励
     * @param clientInviteProgressRewardBo 邀请进度奖励对象
     * @return
     */
    public R<String> update(ClientInviteProgressRewardBo clientInviteProgressRewardBo) {

        clientInviteProgressRewardProducer.update(clientInviteProgressRewardBo);
        return R.ok("修改成功");
    }

    /**
     * 删除邀请进度奖励
     * @param id 邀请进度奖励id
     * @return
     */
    public R<String> delete(Long id) {

        clientInviteProgressRewardProducer.deleteById(id);
        return R.ok("删除成功");
    }


}

