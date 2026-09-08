package com.jiuyu.replay.api.logic.power;

import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;

import com.jiuyu.replay.power.vo.UserTagListVo;
import com.jiuyu.replay.power.vo.UserTagInfoVo;
import com.jiuyu.replay.power.bo.UserTagBo;
import com.jiuyu.replay.power.bo.UserTagListBo;


/**
 * 用户-标签-关联
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-09-20 10:21:25
 */
public interface UserTagLogic {


    /**
     * 用户-标签-关联列表
     * @param userTagListBo 用户-标签-关联列表查询参数
     * @return
     */
    R<PageUtils<UserTagListVo>> queryPage(UserTagListBo userTagListBo);

    /**
    * 用户-标签-关联信息
    * @param id 用户-标签-关联id
    * @return
    */
    R<UserTagInfoVo> info(Long id);

    /**
     * 新增用户-标签-关联
     * @param userTagBo 用户-标签-关联对象
     * @return
     */
    R<String> save(UserTagBo userTagBo);

    /**
     * 修改用户-标签-关联
     * @param userTagBo 用户-标签-关联对象
     * @return
     */
    R<String> update(UserTagBo userTagBo);

    /**
     * 删除用户-标签-关联
     * @param id 用户-标签-关联id
     * @return
     */
    R<String> delete(Long id);


}

