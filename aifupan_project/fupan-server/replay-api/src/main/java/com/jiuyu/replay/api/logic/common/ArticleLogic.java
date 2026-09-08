package com.jiuyu.replay.api.logic.common;

import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;

import com.jiuyu.replay.common.vo.ArticleListVo;
import com.jiuyu.replay.common.vo.ArticleInfoVo;
import com.jiuyu.replay.common.bo.ArticleBo;
import com.jiuyu.replay.common.bo.ArticleListBo;


/**
 * 文章
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-08-23 20:39:45
 */
public interface ArticleLogic {


    /**
     * 文章列表
     * @param articleListBo 文章列表查询参数
     * @return
     */
    R<PageUtils<ArticleListVo>> queryPage(ArticleListBo articleListBo);

    /**
    * 文章信息
    * @param id 文章id
    * @return
    */
    R<ArticleInfoVo> info(Long id);

    /**
     * 新增文章
     * @param articleBo 文章对象
     * @return
     */
    R<String> save(ArticleBo articleBo);

    /**
     * 修改文章
     * @param articleBo 文章对象
     * @return
     */
    R<String> update(ArticleBo articleBo);

    /**
     * 删除文章
     * @param id 文章id
     * @return
     */
    R<String> delete(Long id);


}

