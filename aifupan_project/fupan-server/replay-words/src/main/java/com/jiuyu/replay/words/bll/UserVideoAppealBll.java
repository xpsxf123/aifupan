package com.jiuyu.replay.words.bll;

import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;

import com.jiuyu.replay.words.vo.UserVideoAppealListVo;
import com.jiuyu.replay.words.vo.UserVideoAppealInfoVo;
import com.jiuyu.replay.words.bo.UserVideoAppealBo;
import com.jiuyu.replay.words.bo.UserVideoAppealListBo;
import com.jiuyu.replay.words.producer.UserVideoAppealProducer;

import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;


/**
 * 用户视频申述表
 *
 * @author lj
 * @email xxxxxxx@qq.com
 * @date 2024-11-14 12:12:11
 */
@Component
public class UserVideoAppealBll {

    @Resource
    private UserVideoAppealProducer userVideoAppealProducer;


    /**
     * 用户视频申述表列表
     * @param userVideoAppealListBo 用户视频申述表列表查询参数
     * @return
     */
    public R<PageUtils<UserVideoAppealListVo>> queryPage(UserVideoAppealListBo userVideoAppealListBo) {

        return R.ok("获取成功", userVideoAppealProducer.queryPage(userVideoAppealListBo));
    }

    /**
    * 用户视频申述表信息
    * @param id 用户视频申述表id
    * @return
    */
    public R<UserVideoAppealInfoVo> info(Long id) {

        UserVideoAppealInfoVo userVideoAppealInfoVo = userVideoAppealProducer.info(id);
        return R.ok("获取成功", userVideoAppealInfoVo);
    }

    /**
     * 新增用户视频申述表
     * @param userVideoAppealBo 用户视频申述表对象
     * @return
     */
    public R<String> save(UserVideoAppealBo userVideoAppealBo) {

        UserVideoAppealInfoVo userVideoAppealInfoVo = userVideoAppealProducer.save(userVideoAppealBo);
        return R.ok("提交成功");
    }

    /**
     * 修改用户视频申述表
     * @param userVideoAppealBo 用户视频申述表对象
     * @return
     */
    public R<String> update(UserVideoAppealBo userVideoAppealBo) {

        userVideoAppealProducer.update(userVideoAppealBo);
        return R.ok("修改成功");
    }

    /**
     * 删除用户视频申述表
     * @param id 用户视频申述表id
     * @return
     */
    public R<String> delete(Long id) {

        userVideoAppealProducer.deleteById(id);
        return R.ok("删除成功");
    }


}

