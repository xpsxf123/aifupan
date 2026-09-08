package com.jiuyu.replay.words.producer;

import com.jiuyu.replay.generic.utils.PageUtils;

import com.jiuyu.replay.words.vo.UserVideoAppealListVo;
import com.jiuyu.replay.words.vo.UserVideoAppealInfoVo;
import com.jiuyu.replay.words.bo.UserVideoAppealBo;
import com.jiuyu.replay.words.bo.UserVideoAppealListBo;


/**
 * 用户视频申述表
 *
 * @author lj
 * @email xxxxxxx@qq.com
 * @date 2024-11-14 12:12:11
 */
public interface UserVideoAppealProducer {


    /**
     * 用户视频申述表列表
     * @param userVideoAppealListBo 用户视频申述表列表查询参数
     * @return
     */
    PageUtils<UserVideoAppealListVo> queryPage(UserVideoAppealListBo userVideoAppealListBo);

    /**
    * 用户视频申述表信息
    * @param id 用户视频申述表id
    * @return
    */
    UserVideoAppealInfoVo info(Long id);

    /**
     * 新增用户视频申述表
     * @param userVideoAppealBo 用户视频申述表对象
     * @return
     */
     UserVideoAppealInfoVo save(UserVideoAppealBo userVideoAppealBo);

    /**
     * 修改用户视频申述表
     * @param userVideoAppealBo 用户视频申述表对象
     * @return
     */
    void update(UserVideoAppealBo userVideoAppealBo);

    /**
     * 删除用户视频申述表
     * @param id 用户视频申述表id
     * @return
     */
    void deleteById(Long id);


}

