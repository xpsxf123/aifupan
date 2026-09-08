package com.jiuyu.replay.agent.bll;

import com.jiuyu.replay.agent.bo.ClientInviteRewardRecordDetailBo;
import com.jiuyu.replay.agent.bo.ClientInviteRewardRecordDetailListBo;
import com.jiuyu.replay.agent.producer.ClientInviteRewardRecordDetailProducer;
import com.jiuyu.replay.agent.vo.ClientInviteRewardRecordDetailInfoVo;
import com.jiuyu.replay.agent.vo.ClientInviteRewardRecordDetailListVo;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import jakarta.annotation.Resource;


/**
 * 邀请奖励明细记录
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-05-20 10:36:55
 */
//@Component
public class ClientInviteRewardRecordDetailBll {

    @Resource
    private ClientInviteRewardRecordDetailProducer clientInviteRewardRecordDetailProducer;


    /**
     * 邀请奖励明细记录列表
     * @param clientInviteRewardRecordDetailListBo 邀请奖励明细记录列表查询参数
     * @return
     */
    public R<PageUtils<ClientInviteRewardRecordDetailListVo>> queryPage(ClientInviteRewardRecordDetailListBo clientInviteRewardRecordDetailListBo) {

        return R.ok("获取成功", clientInviteRewardRecordDetailProducer.queryPage(clientInviteRewardRecordDetailListBo));
    }

    /**
    * 邀请奖励明细记录信息
    * @param id 邀请奖励明细记录id
    * @return
    */
    public R<ClientInviteRewardRecordDetailInfoVo> info(Long id) {

        ClientInviteRewardRecordDetailInfoVo clientInviteRewardRecordDetailInfoVo = clientInviteRewardRecordDetailProducer.info(id);
        return R.ok("获取成功", clientInviteRewardRecordDetailInfoVo);
    }

    /**
     * 新增邀请奖励明细记录
     * @param clientInviteRewardRecordDetailBo 邀请奖励明细记录对象
     * @return
     */
    public R<String> save(ClientInviteRewardRecordDetailBo clientInviteRewardRecordDetailBo) {

        ClientInviteRewardRecordDetailInfoVo clientInviteRewardRecordDetailInfoVo = clientInviteRewardRecordDetailProducer.save(clientInviteRewardRecordDetailBo);
        return R.ok("添加成功");
    }

    /**
     * 修改邀请奖励明细记录
     * @param clientInviteRewardRecordDetailBo 邀请奖励明细记录对象
     * @return
     */
    public R<String> update(ClientInviteRewardRecordDetailBo clientInviteRewardRecordDetailBo) {

        clientInviteRewardRecordDetailProducer.update(clientInviteRewardRecordDetailBo);
        return R.ok("修改成功");
    }

    /**
     * 删除邀请奖励明细记录
     * @param id 邀请奖励明细记录id
     * @return
     */
    public R<String> delete(Long id) {

        clientInviteRewardRecordDetailProducer.deleteById(id);
        return R.ok("删除成功");
    }


}

