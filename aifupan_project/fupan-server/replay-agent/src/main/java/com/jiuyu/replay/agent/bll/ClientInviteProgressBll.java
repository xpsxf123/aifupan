package com.jiuyu.replay.agent.bll;

import com.jiuyu.replay.agent.bo.ClientInviteProgressBo;
import com.jiuyu.replay.agent.bo.ClientInviteProgressListBo;
import com.jiuyu.replay.agent.producer.ClientInviteProgressProducer;
import com.jiuyu.replay.agent.vo.ClientInviteProgressInfoVo;
import com.jiuyu.replay.agent.vo.ClientInviteProgressListVo;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import jakarta.annotation.Resource;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


/**
 * 邀请进度
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-05-20 10:36:55
 */
//@Component
public class ClientInviteProgressBll {

    @Resource
    private ClientInviteProgressProducer clientInviteProgressProducer;


    /**
     * 邀请进度列表
     * @param clientInviteProgressListBo 邀请进度列表查询参数
     * @return
     */
    public R<PageUtils<ClientInviteProgressListVo>> queryPage(ClientInviteProgressListBo clientInviteProgressListBo) {

        return R.ok("获取成功", clientInviteProgressProducer.queryPage(clientInviteProgressListBo));
    }

    /**
    * 邀请进度信息
    * @param id 邀请进度id
    * @return
    */
    public R<ClientInviteProgressInfoVo> info(Long id) {

        ClientInviteProgressInfoVo clientInviteProgressInfoVo = clientInviteProgressProducer.info(id);
        return R.ok("获取成功", clientInviteProgressInfoVo);
    }

    /**
     * 新增邀请进度
     * @param clientInviteProgressBo 邀请进度对象
     * @return
     */
    public R<String> save(ClientInviteProgressBo clientInviteProgressBo) {

        ClientInviteProgressInfoVo clientInviteProgressInfoVo = clientInviteProgressProducer.save(clientInviteProgressBo);
        return R.ok("添加成功");
    }

    /**
     * 修改邀请进度
     * @param clientInviteProgressBo 邀请进度对象
     * @return
     */
    public R<String> update(ClientInviteProgressBo clientInviteProgressBo) {

        clientInviteProgressProducer.update(clientInviteProgressBo);
        return R.ok("修改成功");
    }

    /**
     * 删除邀请进度
     * @param id 邀请进度id
     * @return
     */
    public R<String> delete(Long id) {

        clientInviteProgressProducer.deleteById(id);
        return R.ok("删除成功");
    }


    /**
     * 设置邀请进度和进度奖励
     * @param clientInviteProgressBoList 进度集合
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    public R<String> setInviteProgressAndReward(List<ClientInviteProgressBo> clientInviteProgressBoList) {

//        // 将旧的所有进度状态改为停用
//        this.clientInviteProgressProducer.disableOld();
//        // 保存新的进度记录
//        this.clientInviteProgressProducer.saveBatch(clientInviteProgressBoList);

        return R.ok();
    }

    /**
     * 获取邀请进度和进度奖励
     * @param inviteActivityId 活动id
     * @param inviteProgressType 进度类型 0：被邀请人进度 1：邀请人进度
     * @return
     */
    public R<List<ClientInviteProgressInfoVo>> getInviteProgressAndReward(Long inviteActivityId, Integer inviteProgressType) {

        // 获取所有进度和奖励
        List<ClientInviteProgressInfoVo> progressInfoVos = this.clientInviteProgressProducer.listAllActivity(inviteActivityId, inviteProgressType);

        return R.ok(progressInfoVos);
    }
}

