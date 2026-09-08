package com.jiuyu.replay.api.logic.power;

import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;

import com.jiuyu.replay.power.vo.UserRemarkListVo;
import com.jiuyu.replay.power.vo.UserRemarkInfoVo;
import com.jiuyu.replay.power.bo.UserRemarkBo;
import com.jiuyu.replay.power.bo.UserRemarkListBo;


/**
 * 用户备注表
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-02-12 15:59:28
 */
public interface UserRemarkLogic {


    /**
     * 用户备注表列表
     * @param userRemarkListBo 用户备注表列表查询参数
     * @return
     */
    R<PageUtils<UserRemarkListVo>> queryPage(UserRemarkListBo userRemarkListBo);

    /**
    * 用户备注表信息
    * @param id 用户备注表id
    * @return
    */
    R<UserRemarkInfoVo> info(Long id);

    /**
     * 新增用户备注表
     * @param userRemarkBo 用户备注表对象
     * @return
     */
    R<String> save(UserRemarkBo userRemarkBo);

    /**
     * 修改用户备注表
     * @param userRemarkBo 用户备注表对象
     * @return
     */
    R<String> update(UserRemarkBo userRemarkBo);

    /**
     * 删除用户备注表
     * @param id 用户备注表id
     * @return
     */
    R<String> delete(Long id);


}

