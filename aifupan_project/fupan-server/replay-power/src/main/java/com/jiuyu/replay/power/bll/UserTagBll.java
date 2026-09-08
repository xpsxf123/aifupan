package com.jiuyu.replay.power.bll;

import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;

import com.jiuyu.replay.power.vo.UserTagListVo;
import com.jiuyu.replay.power.vo.UserTagInfoVo;
import com.jiuyu.replay.power.bo.UserTagBo;
import com.jiuyu.replay.power.bo.UserTagListBo;
import com.jiuyu.replay.power.producer.UserTagProducer;

import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;


/**
 * 用户-标签-关联
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-09-20 10:21:25
 */
@Component
public class UserTagBll {

    @Resource
    private UserTagProducer userTagProducer;


    /**
     * 用户-标签-关联列表
     * @param userTagListBo 用户-标签-关联列表查询参数
     * @return
     */
    public R<PageUtils<UserTagListVo>> queryPage(UserTagListBo userTagListBo) {

        return R.ok("获取成功", userTagProducer.queryPage(userTagListBo));
    }

    /**
    * 用户-标签-关联信息
    * @param id 用户-标签-关联id
    * @return
    */
    public R<UserTagInfoVo> info(Long id) {

        UserTagInfoVo userTagInfoVo = userTagProducer.info(id);
        return R.ok("获取成功", userTagInfoVo);
    }

    /**
     * 新增用户-标签-关联
     * @param userTagBo 用户-标签-关联对象
     * @return
     */
    public R<String> save(UserTagBo userTagBo) {

        UserTagInfoVo userTagInfoVo = userTagProducer.save(userTagBo);
        return R.ok("添加成功");
    }

    /**
     * 修改用户-标签-关联
     * @param userTagBo 用户-标签-关联对象
     * @return
     */
    public R<String> update(UserTagBo userTagBo) {

        userTagProducer.update(userTagBo);
        return R.ok("修改成功");
    }

    /**
     * 删除用户-标签-关联
     * @param id 用户-标签-关联id
     * @return
     */
    public R<String> delete(Long id) {

        userTagProducer.deleteById(id);
        return R.ok("删除成功");
    }


}

