package com.jiuyu.replay.api.logic.words;

import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;

import com.jiuyu.replay.words.bo.UserVideoAppealClientBo;
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
public interface UserVideoAppealLogic {


    /**
     * 用户视频申述表列表
     * @param userVideoAppealListBo 用户视频申述表列表查询参数
     * @return
     */
    R<PageUtils<UserVideoAppealListVo>> queryPage(UserVideoAppealListBo userVideoAppealListBo);

    /**
    * 用户视频申述表信息
    * @param id 用户视频申述表id
    * @return
    */
    R<UserVideoAppealInfoVo> info(Long id);

    /**
     * 新增用户视频申述表
     * @param userVideoAppealBo 用户视频申述表对象
     * @return
     */
    R<String> save(UserVideoAppealBo userVideoAppealBo);

    /**
     * 修改用户视频申述表
     * @param userVideoAppealBo 用户视频申述表对象
     * @return
     */
    R<String> update(UserVideoAppealBo userVideoAppealBo);

    /**
     * 删除用户视频申述表
     * @param id 用户视频申述表id
     * @return
     */
    R<String> delete(Long id);

    /**
     * 用户上传视频申述
     * @param bo
     * @return
     */
    R<String> uploadVideoAppeal(UserVideoAppealClientBo bo);

    /**
     * 处理用户视频申述
     * @param userVideoAppealBo
     * @return
     */
    R<String> handleAppeal(UserVideoAppealBo userVideoAppealBo);
}

