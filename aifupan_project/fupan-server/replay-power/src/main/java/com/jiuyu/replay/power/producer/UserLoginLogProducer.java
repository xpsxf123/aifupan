package com.jiuyu.replay.power.producer;

import com.jiuyu.replay.generic.utils.PageUtils;

import com.jiuyu.replay.power.vo.UserLoginLogListVo;
import com.jiuyu.replay.power.vo.UserLoginLogInfoVo;
import com.jiuyu.replay.power.bo.UserLoginLogBo;
import com.jiuyu.replay.power.bo.UserLoginLogListBo;


/**
 * 用户登录日志
 *
 * @author lj
 * @email xxxxxxx@qq.com
 * @date 2024-11-15 18:48:54
 */
public interface UserLoginLogProducer {


    /**
     * 用户登录日志列表
     * @param userLoginLogListBo 用户登录日志列表查询参数
     * @return
     */
    PageUtils<UserLoginLogListVo> queryPage(UserLoginLogListBo userLoginLogListBo);

    /**
    * 用户登录日志信息
    * @param id 用户登录日志id
    * @return
    */
    UserLoginLogInfoVo info(Long id);

    /**
     * 新增用户登录日志
     * @param userLoginLogBo 用户登录日志对象
     * @return
     */
     UserLoginLogInfoVo save(UserLoginLogBo userLoginLogBo);

    /**
     * 修改用户登录日志
     * @param userLoginLogBo 用户登录日志对象
     * @return
     */
    void update(UserLoginLogBo userLoginLogBo);

    /**
     * 删除用户登录日志
     * @param id 用户登录日志id
     * @return
     */
    void deleteById(Long id);

    /**
     * Data Hub：按用户批量聚合成功登录记录
     *
     * @param userIds   用户id列表
     * @param startDate 窗口起（含，可空）
     * @param endDate   窗口止（不含，可空）
     *
     * @return {@link java.util.List }<{@link com.jiuyu.replay.power.vo.datahub.DataHubLoginStatsVo }>
     */
    java.util.List<com.jiuyu.replay.power.vo.datahub.DataHubLoginStatsVo> aggregateSuccessLogin(
            java.util.Collection<Long> userIds, java.util.Date startDate, java.util.Date endDate);
}

