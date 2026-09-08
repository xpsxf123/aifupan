package com.jiuyu.replay.api.logic.power.impl;

import com.jiuyu.replay.api.logic.power.TagLogic;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.power.bll.TagBll;
import com.jiuyu.replay.power.bo.TagBo;
import com.jiuyu.replay.power.bo.TagListBo;
import com.jiuyu.replay.power.vo.TagInfoVo;
import com.jiuyu.replay.power.vo.TagListVo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;


/**
 * 用户标签
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-09-20 10:21:25
 */
@Service
public class TagLogicImpl implements TagLogic {

    @Resource
    private TagBll tagBll;


    @Override
    public R<PageUtils<TagListVo>> queryPage(TagListBo tagListBo) {

        return tagBll.queryPage(tagListBo);
    }

    @Override
    public R<TagInfoVo> info(Long id) {

        return tagBll.info(id);
    }

    @Override
    public R<String> save(TagBo tagBo) {

        return tagBll.save(tagBo);
    }

    @Override
    public R<String> update(TagBo tagBo) {

        return tagBll.update(tagBo);
    }

    @Override
    public R<String> delete(Long id) {

        return tagBll.delete(id);
    }


}

