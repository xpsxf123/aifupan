package com.jiuyu.replay.api.logic.power;

import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.power.bo.UserDetailsBo;

/**
 * 用户详情表
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-09-14 10:12:41
 */
public interface UserDetailsLogic {


    /**
     * 客户端保存或修改用户详情接口
     * @param userDetailsBo
     * @return
     */
    R<String> saveUserDetails(UserDetailsBo userDetailsBo);
}

