package com.jiuyu.replay.api.logic.power;

import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;

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
public interface UserLoginLogLogic {


    /**
     * 用户登录日志列表
     * @param userLoginLogListBo 用户登录日志列表查询参数
     * @return
     */
    R<PageUtils<UserLoginLogListVo>> queryPage(UserLoginLogListBo userLoginLogListBo);

    /**
    * 用户登录日志信息
    * @param id 用户登录日志id
    * @return
    */
    R<UserLoginLogInfoVo> info(Long id);

    /**
     * 新增用户登录日志
     * @param userLoginLogBo 用户登录日志对象
     * @return
     */
    R<String> save(UserLoginLogBo userLoginLogBo);

    /**
     * 修改用户登录日志
     * @param userLoginLogBo 用户登录日志对象
     * @return
     */
    R<String> update(UserLoginLogBo userLoginLogBo);

    /**
     * 删除用户登录日志
     * @param id 用户登录日志id
     * @return
     */
    R<String> delete(Long id);


}

