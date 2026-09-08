package com.jiuyu.replay.power.bll;

import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;

import com.jiuyu.replay.power.vo.TagListVo;
import com.jiuyu.replay.power.vo.TagInfoVo;
import com.jiuyu.replay.power.bo.TagBo;
import com.jiuyu.replay.power.bo.TagListBo;
import com.jiuyu.replay.power.producer.TagProducer;

import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;


/**
 * 用户标签
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-09-20 10:21:25
 */
@Component
public class TagBll {

    @Resource
    private TagProducer tagProducer;


    /**
     * 用户标签列表
     * @param tagListBo 用户标签列表查询参数
     * @return
     */
    public R<PageUtils<TagListVo>> queryPage(TagListBo tagListBo) {

        return R.ok("获取成功", tagProducer.queryPage(tagListBo));
    }

    /**
    * 用户标签信息
    * @param id 用户标签id
    * @return
    */
    public R<TagInfoVo> info(Long id) {

        TagInfoVo tagInfoVo = tagProducer.info(id);
        return R.ok("获取成功", tagInfoVo);
    }

    /**
     * 新增用户标签
     * @param tagBo 用户标签对象
     * @return
     */
    public R<String> save(TagBo tagBo) {

        TagInfoVo tagInfoVo = tagProducer.save(tagBo);
        return R.ok("添加成功");
    }

    /**
     * 修改用户标签
     * @param tagBo 用户标签对象
     * @return
     */
    public R<String> update(TagBo tagBo) {

        tagProducer.update(tagBo);
        return R.ok("修改成功");
    }

    /**
     * 删除用户标签
     * @param id 用户标签id
     * @return
     */
    public R<String> delete(Long id) {

        tagProducer.deleteById(id);
        return R.ok("删除成功");
    }


}

