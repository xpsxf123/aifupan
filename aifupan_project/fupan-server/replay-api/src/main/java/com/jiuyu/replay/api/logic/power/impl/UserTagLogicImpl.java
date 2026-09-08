package com.jiuyu.replay.api.logic.power.impl;

import com.jiuyu.replay.api.logic.power.UserTagLogic;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.power.bll.UserTagBll;
import com.jiuyu.replay.power.bo.UserTagBo;
import com.jiuyu.replay.power.bo.UserTagListBo;
import com.jiuyu.replay.power.vo.UserTagInfoVo;
import com.jiuyu.replay.power.vo.UserTagListVo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;


/**
 * 用户-标签-关联
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-09-20 10:21:25
 */
@Service
public class UserTagLogicImpl implements UserTagLogic {

    @Resource
    private UserTagBll userTagBll;


    @Override
    public R<PageUtils<UserTagListVo>> queryPage(UserTagListBo userTagListBo) {

        return userTagBll.queryPage(userTagListBo);
    }

    @Override
    public R<UserTagInfoVo> info(Long id) {

        return userTagBll.info(id);
    }

    @Override
    public R<String> save(UserTagBo userTagBo) {

        return userTagBll.save(userTagBo);
    }

    @Override
    public R<String> update(UserTagBo userTagBo) {

        return userTagBll.update(userTagBo);
    }

    @Override
    public R<String> delete(Long id) {

        return userTagBll.delete(id);
    }


}

