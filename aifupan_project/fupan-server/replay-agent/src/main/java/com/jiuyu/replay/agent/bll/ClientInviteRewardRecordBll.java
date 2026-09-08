package com.jiuyu.replay.agent.bll;

import com.jiuyu.replay.agent.bo.ClientInviteRewardRecordBo;
import com.jiuyu.replay.agent.bo.ClientInviteRewardRecordListBo;
import com.jiuyu.replay.agent.producer.ClientInviteRewardRecordProducer;
import com.jiuyu.replay.agent.vo.ClientInviteRewardRecordInfoVo;
import com.jiuyu.replay.agent.vo.ClientInviteRewardRecordListVo;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import jakarta.annotation.Resource;


/**
 * 邀请奖励记录
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-05-20 10:36:55
 */
//@Component
public class ClientInviteRewardRecordBll {

    @Resource
    private ClientInviteRewardRecordProducer clientInviteRewardRecordProducer;


    /**
     * 邀请奖励记录列表
     * @param clientInviteRewardRecordListBo 邀请奖励记录列表查询参数
     * @return
     */
    public R<PageUtils<ClientInviteRewardRecordListVo>> queryPage(ClientInviteRewardRecordListBo clientInviteRewardRecordListBo) {

        return R.ok("获取成功", clientInviteRewardRecordProducer.queryPage(clientInviteRewardRecordListBo));
    }

    /**
    * 邀请奖励记录信息
    * @param id 邀请奖励记录id
    * @return
    */
    public R<ClientInviteRewardRecordInfoVo> info(Long id) {

        ClientInviteRewardRecordInfoVo clientInviteRewardRecordInfoVo = clientInviteRewardRecordProducer.info(id);
        return R.ok("获取成功", clientInviteRewardRecordInfoVo);
    }

    /**
     * 新增邀请奖励记录
     * @param clientInviteRewardRecordBo 邀请奖励记录对象
     * @return
     */
    public R<String> save(ClientInviteRewardRecordBo clientInviteRewardRecordBo) {

        ClientInviteRewardRecordInfoVo clientInviteRewardRecordInfoVo = clientInviteRewardRecordProducer.save(clientInviteRewardRecordBo);
        return R.ok("添加成功");
    }

    /**
     * 修改邀请奖励记录
     * @param clientInviteRewardRecordBo 邀请奖励记录对象
     * @return
     */
    public R<String> update(ClientInviteRewardRecordBo clientInviteRewardRecordBo) {

        clientInviteRewardRecordProducer.update(clientInviteRewardRecordBo);
        return R.ok("修改成功");
    }

    /**
     * 删除邀请奖励记录
     * @param id 邀请奖励记录id
     * @return
     */
    public R<String> delete(Long id) {

        clientInviteRewardRecordProducer.deleteById(id);
        return R.ok("删除成功");
    }


}

