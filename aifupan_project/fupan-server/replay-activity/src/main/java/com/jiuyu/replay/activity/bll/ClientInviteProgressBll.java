package com.jiuyu.replay.activity.bll;

import com.jiuyu.replay.activity.rse.ClientInviteProgressRse;
import com.jiuyu.replay.generic.bo.activity.ClientInviteProgressBo;
import com.jiuyu.replay.generic.bo.activity.ClientInviteProgressListBo;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.activity.ClientInviteProgressInfoVo;
import com.jiuyu.replay.generic.vo.activity.ClientInviteProgressListVo;
import com.jiuyu.replay.generic.vo.common.R;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.List;


/**
 * 邀请进度
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-05-24 16:10:28
 */
@Component
public class ClientInviteProgressBll {

    @Resource
    private ClientInviteProgressRse clientInviteProgressRse;


    /**
     * 邀请进度列表
     *
     * @param clientInviteProgressListBo 邀请进度列表查询参数
     * @return
     */
    public R<PageUtils<ClientInviteProgressListVo>> queryPage(ClientInviteProgressListBo clientInviteProgressListBo) {

        return R.ok("获取成功", clientInviteProgressRse.queryPage(clientInviteProgressListBo));
    }

    /**
     * 邀请进度信息
     *
     * @param id 邀请进度id
     * @return
     */
    public R<ClientInviteProgressInfoVo> info(Long id) {

        ClientInviteProgressInfoVo clientInviteProgressInfoVo = clientInviteProgressRse.info(id);
        return R.ok("获取成功", clientInviteProgressInfoVo);
    }

    /**
     * 新增邀请进度
     *
     * @param clientInviteProgressBo 邀请进度对象
     * @return
     */
    public R<String> save(ClientInviteProgressBo clientInviteProgressBo) {

        ClientInviteProgressInfoVo clientInviteProgressInfoVo = clientInviteProgressRse.save(clientInviteProgressBo);
        return R.ok("添加成功");
    }

    /**
     * 修改邀请进度
     *
     * @param clientInviteProgressBo 邀请进度对象
     * @return
     */
    public R<String> update(ClientInviteProgressBo clientInviteProgressBo) {

        clientInviteProgressRse.update(clientInviteProgressBo);
        return R.ok("修改成功");
    }

    /**
     * 删除邀请进度
     *
     * @param id 邀请进度id
     * @return
     */
    public R<String> delete(Long id) {

        clientInviteProgressRse.deleteById(id);
        return R.ok("删除成功");
    }

    /**
     * 获取活动进度根据活动id和进度类型
     *
     * @param activityId         活动id
     * @param inviteProgressType 进度类型 0：被邀请人进度 1：邀请人进度
     * @return
     */
    public List<ClientInviteProgressBo> listClientInviteProgressByActivityIdAndInviteProgressType(Long activityId, Integer inviteProgressType) {
        return clientInviteProgressRse.listClientInviteProgressByActivityIdAndInviteProgressType(activityId, inviteProgressType);
    }
}

