package com.jiuyu.replay.power.producer;

import com.jiuyu.replay.generic.utils.PageUtils;

import com.jiuyu.replay.power.vo.TagListVo;
import com.jiuyu.replay.power.vo.TagInfoVo;
import com.jiuyu.replay.power.bo.TagBo;
import com.jiuyu.replay.power.bo.TagListBo;


/**
 * 用户标签
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-09-20 10:21:25
 */
public interface TagProducer {


    /**
     * 用户标签列表
     * @param tagListBo 用户标签列表查询参数
     * @return
     */
    PageUtils<TagListVo> queryPage(TagListBo tagListBo);

    /**
    * 用户标签信息
    * @param id 用户标签id
    * @return
    */
    TagInfoVo info(Long id);

    /**
     * 新增用户标签
     * @param tagBo 用户标签对象
     * @return
     */
     TagInfoVo save(TagBo tagBo);

    /**
     * 修改用户标签
     * @param tagBo 用户标签对象
     * @return
     */
    void update(TagBo tagBo);

    /**
     * 删除用户标签
     * @param id 用户标签id
     * @return
     */
    void deleteById(Long id);


}

