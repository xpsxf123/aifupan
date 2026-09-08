package com.jiuyu.replay.power.bll;

import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;

import com.jiuyu.replay.power.vo.UserLoginLogListVo;
import com.jiuyu.replay.power.vo.UserLoginLogInfoVo;
import com.jiuyu.replay.power.bo.UserLoginLogBo;
import com.jiuyu.replay.power.bo.UserLoginLogListBo;
import com.jiuyu.replay.power.producer.UserLoginLogProducer;

import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;


/**
 * 用户登录日志
 *
 * @author lj
 * @email xxxxxxx@qq.com
 * @date 2024-11-15 18:48:54
 */
@Component
public class UserLoginLogBll {

    @Resource
    private UserLoginLogProducer userLoginLogProducer;


    /**
     * 用户登录日志列表
     * @param userLoginLogListBo 用户登录日志列表查询参数
     * @return
     */
    public R<PageUtils<UserLoginLogListVo>> queryPage(UserLoginLogListBo userLoginLogListBo) {

        return R.ok("获取成功", userLoginLogProducer.queryPage(userLoginLogListBo));
    }

    /**
    * 用户登录日志信息
    * @param id 用户登录日志id
    * @return
    */
    public R<UserLoginLogInfoVo> info(Long id) {

        UserLoginLogInfoVo userLoginLogInfoVo = userLoginLogProducer.info(id);
        return R.ok("获取成功", userLoginLogInfoVo);
    }

    /**
     * 新增用户登录日志
     * @param userLoginLogBo 用户登录日志对象
     * @return
     */
    public R<String> save(UserLoginLogBo userLoginLogBo) {

        UserLoginLogInfoVo userLoginLogInfoVo = userLoginLogProducer.save(userLoginLogBo);
        return R.ok("添加成功");
    }

    /**
     * 修改用户登录日志
     * @param userLoginLogBo 用户登录日志对象
     * @return
     */
    public R<String> update(UserLoginLogBo userLoginLogBo) {

        userLoginLogProducer.update(userLoginLogBo);
        return R.ok("修改成功");
    }

    /**
     * 删除用户登录日志
     * @param id 用户登录日志id
     * @return
     */
    public R<String> delete(Long id) {

        userLoginLogProducer.deleteById(id);
        return R.ok("删除成功");
    }


}

