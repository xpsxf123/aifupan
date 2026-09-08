package com.jiuyu.replay.power.producer;

import com.jiuyu.replay.generic.utils.PageUtils;

import com.jiuyu.replay.power.vo.UserRemarkListVo;
import com.jiuyu.replay.power.vo.UserRemarkInfoVo;
import com.jiuyu.replay.power.bo.UserRemarkBo;
import com.jiuyu.replay.power.bo.UserRemarkListBo;
import com.jiuyu.replay.power.vo.LatestRemarkVo;

import java.util.Collection;
import java.util.Map;


/**
 * 用户备注表
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-02-12 15:59:28
 */
public interface UserRemarkProducer {


    /**
     * 用户备注表列表
     * @param userRemarkListBo 用户备注表列表查询参数
     * @return
     */
    PageUtils<UserRemarkListVo> queryPage(UserRemarkListBo userRemarkListBo);

    /**
    * 用户备注表信息
    * @param id 用户备注表id
    * @return
    */
    UserRemarkInfoVo info(Long id);

    /**
     * 新增用户备注表（已改成跟进记录）
     * @param userRemarkBo 用户备注表对象
     * @return
     */
     void save(UserRemarkBo userRemarkBo);

    /**
     * 修改用户备注表
     * @param userRemarkBo 用户备注表对象
     * @return
     */
    void update(UserRemarkBo userRemarkBo);

    /**
     * 删除用户备注表
     * @param id 用户备注表id
     * @return
     */
    void deleteById(Long id);

    /**
     * 同步用户最新跟进记录到业务表
     *
     * @param userId 用户ID
     */
    void syncLatestRemarkToBusiness(Long userId);

    /**
     * 批量获取多个用户各自的最新一条跟进记录
     * 「最新」口径 = create_date DESC，tie-break id DESC；跟进人取 create_id 对应的 nickName
     *
     * @param userIds 用户ID集合，为空时返回空 Map 且不发起任何查询
     *
     * @return 用户ID → 最新跟进；无跟进的用户不出现在结果中
     */
    Map<Long, LatestRemarkVo> batchLatestByUserIds(Collection<Long> userIds);


}

