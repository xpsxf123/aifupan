package com.jiuyu.replay.api.logic.common.impl;

import com.jiuyu.replay.api.logic.common.ArticleLogic;
import com.jiuyu.replay.common.bll.ArticleBll;
import com.jiuyu.replay.common.bo.ArticleBo;
import com.jiuyu.replay.common.bo.ArticleListBo;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.common.vo.ArticleInfoVo;
import com.jiuyu.replay.common.vo.ArticleListVo;
import com.jiuyu.replay.power.utils.GlobalObject;
import com.jiuyu.replay.power.vo.UserCacheVo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;


/**
 * 文章
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-08-23 20:39:45
 */
@Service
public class ArticleLogicImpl implements ArticleLogic {

    @Resource
    private ArticleBll articleBll;


    @Override
    public R<PageUtils<ArticleListVo>> queryPage(ArticleListBo articleListBo) {

        return articleBll.queryPage(articleListBo);
    }

    @Override
    public R<ArticleInfoVo> info(Long id) {

        return articleBll.info(id);
    }

    @Override
    public R<String> save(ArticleBo articleBo) {
        UserCacheVo user = GlobalObject.getLocalUser();
        articleBo.setUserId(user.getId());

        return articleBll.save(articleBo);
    }

    @Override
    public R<String> update(ArticleBo articleBo) {

        return articleBll.update(articleBo);
    }

    @Override
    public R<String> delete(Long id) {

        return articleBll.delete(id);
    }


}

