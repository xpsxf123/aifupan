package com.jiuyu.replay.api.logic.power.impl;

import com.jiuyu.replay.api.logic.power.UserLoginLogLogic;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.power.bll.UserLoginLogBll;
import com.jiuyu.replay.power.bo.UserLoginLogBo;
import com.jiuyu.replay.power.bo.UserLoginLogListBo;
import com.jiuyu.replay.power.vo.UserLoginLogInfoVo;
import com.jiuyu.replay.power.vo.UserLoginLogListVo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;


/**
 * 用户登录日志
 *
 * @author lj
 * @email xxxxxxx@qq.com
 * @date 2024-11-15 18:48:54
 */
@Service
public class UserLoginLogLogicImpl implements UserLoginLogLogic {

    @Resource
    private UserLoginLogBll userLoginLogBll;


    @Override
    public R<PageUtils<UserLoginLogListVo>> queryPage(UserLoginLogListBo userLoginLogListBo) {

        return userLoginLogBll.queryPage(userLoginLogListBo);
    }

    @Override
    public R<UserLoginLogInfoVo> info(Long id) {

        return userLoginLogBll.info(id);
    }

    @Override
    public R<String> save(UserLoginLogBo userLoginLogBo) {

        return userLoginLogBll.save(userLoginLogBo);
    }

    @Override
    public R<String> update(UserLoginLogBo userLoginLogBo) {

        return userLoginLogBll.update(userLoginLogBo);
    }

    @Override
    public R<String> delete(Long id) {

        return userLoginLogBll.delete(id);
    }


}

