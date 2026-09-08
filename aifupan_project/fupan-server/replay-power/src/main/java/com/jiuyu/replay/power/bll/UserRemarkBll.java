package com.jiuyu.replay.power.bll;

import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;

import com.jiuyu.replay.power.vo.LatestRemarkVo;
import com.jiuyu.replay.power.vo.UserRemarkListVo;
import com.jiuyu.replay.power.vo.UserRemarkInfoVo;
import com.jiuyu.replay.power.bo.UserRemarkBo;
import com.jiuyu.replay.power.bo.UserRemarkListBo;
import com.jiuyu.replay.power.producer.UserRemarkProducer;

import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Map;


/**
 * 用户备注表
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-02-12 15:59:28
 */
@Component
public class UserRemarkBll {

    @Resource
    private UserRemarkProducer userRemarkProducer;


    /**
     * 用户备注表列表
     * @param userRemarkListBo 用户备注表列表查询参数
     * @return
     */
    public R<PageUtils<UserRemarkListVo>> queryPage(UserRemarkListBo userRemarkListBo) {

        return R.ok("获取成功", userRemarkProducer.queryPage(userRemarkListBo));
    }

    /**
    * 用户备注表信息
    * @param id 用户备注表id
    * @return
    */
    public R<UserRemarkInfoVo> info(Long id) {

        UserRemarkInfoVo userRemarkInfoVo = userRemarkProducer.info(id);
        return R.ok("获取成功", userRemarkInfoVo);
    }

    /**
     * 新增用户备注表
     * @param userRemarkBo 用户备注表对象
     * @return
     */
    public R<String> save(UserRemarkBo userRemarkBo) {

        userRemarkProducer.save(userRemarkBo);
        return R.ok("添加成功");
    }

    /**
     * 修改用户备注表
     * @param userRemarkBo 用户备注表对象
     * @return
     */
    public R<String> update(UserRemarkBo userRemarkBo) {

        userRemarkProducer.update(userRemarkBo);
        return R.ok("修改成功");
    }

    /**
     * 删除用户备注表
     * @param id 用户备注表id
     * @return
     */
    public R<String> delete(Long id) {

        userRemarkProducer.deleteById(id);
        return R.ok("删除成功");
    }

    /**
     * 批量获取多个用户各自的最新一条跟进记录
     *
     * @param userIds 用户ID集合
     * @return 用户ID → 最新跟进；无跟进的用户不出现在结果中
     */
    public Map<Long, LatestRemarkVo> batchLatestByUserIds(Collection<Long> userIds) {

        return userRemarkProducer.batchLatestByUserIds(userIds);
    }


}

