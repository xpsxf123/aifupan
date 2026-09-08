package com.jiuyu.replay.activity.bll;

import com.jiuyu.replay.activity.rse.ClientInviteProgressRewardRse;
import com.jiuyu.replay.generic.bo.activity.ClientInviteProgressRewardBo;
import com.jiuyu.replay.generic.bo.activity.ClientInviteProgressRewardListBo;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.activity.ClientInviteProgressRewardInfoVo;
import com.jiuyu.replay.generic.vo.activity.ClientInviteProgressRewardListVo;
import com.jiuyu.replay.generic.vo.common.R;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;


/**
 * 邀请进度奖励
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-05-24 16:10:28
 */
@Component
public class ClientInviteProgressRewardBll {

    @Resource
    private ClientInviteProgressRewardRse clientInviteProgressRewardRse;


    /**
     * 邀请进度奖励列表
     * @param clientInviteProgressRewardListBo 邀请进度奖励列表查询参数
     * @return
     */
    public R<PageUtils<ClientInviteProgressRewardListVo>> queryPage(ClientInviteProgressRewardListBo clientInviteProgressRewardListBo) {

        return R.ok("获取成功", clientInviteProgressRewardRse.queryPage(clientInviteProgressRewardListBo));
    }

    /**
    * 邀请进度奖励信息
    * @param id 邀请进度奖励id
    * @return
    */
    public R<ClientInviteProgressRewardInfoVo> info(Long id) {

        ClientInviteProgressRewardInfoVo clientInviteProgressRewardInfoVo = clientInviteProgressRewardRse.info(id);
        return R.ok("获取成功", clientInviteProgressRewardInfoVo);
    }

    /**
     * 新增邀请进度奖励
     * @param clientInviteProgressRewardBo 邀请进度奖励对象
     * @return
     */
    public R<String> save(ClientInviteProgressRewardBo clientInviteProgressRewardBo) {

        ClientInviteProgressRewardInfoVo clientInviteProgressRewardInfoVo = clientInviteProgressRewardRse.save(clientInviteProgressRewardBo);
        return R.ok("添加成功");
    }

    /**
     * 修改邀请进度奖励
     * @param clientInviteProgressRewardBo 邀请进度奖励对象
     * @return
     */
    public R<String> update(ClientInviteProgressRewardBo clientInviteProgressRewardBo) {

        clientInviteProgressRewardRse.update(clientInviteProgressRewardBo);
        return R.ok("修改成功");
    }

    /**
     * 删除邀请进度奖励
     * @param id 邀请进度奖励id
     * @return
     */
    public R<String> delete(Long id) {

        clientInviteProgressRewardRse.deleteById(id);
        return R.ok("删除成功");
    }

    /**
     * 根据进度奖励id集合获取进度奖励列表
     * @param progressRewardIds 进度奖励id集合
     * @return
     */
    public R<List<ClientInviteProgressRewardInfoVo>> listByProgressRewardIds(Collection<Long> progressRewardIds) {

        List<ClientInviteProgressRewardInfoVo> progressRewardInfoVos = this.clientInviteProgressRewardRse.listByProgressRewardIds(progressRewardIds);

        return R.ok(progressRewardInfoVos);

    }

    /**
     * 获取奖励规则根据进度ids
     * @param progressIds 进度ids
     * @return
     */
    public List<ClientInviteProgressRewardBo> listByProgressIds(List<Long> progressIds) {
        return clientInviteProgressRewardRse.listByProgressIds(progressIds);
    }
}

