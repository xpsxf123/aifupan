package com.jiuyu.replay.common.bll;

import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;

import com.jiuyu.replay.common.vo.ArticleListVo;
import com.jiuyu.replay.common.vo.ArticleInfoVo;
import com.jiuyu.replay.common.bo.ArticleBo;
import com.jiuyu.replay.common.bo.ArticleListBo;
import com.jiuyu.replay.common.producer.ArticleProducer;

import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;


/**
 * 文章
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-08-23 20:39:45
 */
@Component
public class ArticleBll {

    @Resource
    private ArticleProducer articleProducer;


    /**
     * 文章列表
     * @param articleListBo 文章列表查询参数
     * @return
     */
    public R<PageUtils<ArticleListVo>> queryPage(ArticleListBo articleListBo) {

        return R.ok("获取成功", articleProducer.queryPage(articleListBo));
    }

    /**
    * 文章信息
    * @param id 文章id
    * @return
    */
    public R<ArticleInfoVo> info(Long id) {

        ArticleInfoVo articleInfoVo = articleProducer.info(id);
        return R.ok("获取成功", articleInfoVo);
    }

    /**
     * 新增文章
     * @param articleBo 文章对象
     * @return
     */
    public R<String> save(ArticleBo articleBo) {

        ArticleInfoVo articleInfoVo = articleProducer.save(articleBo);
        return R.ok("添加成功");
    }

    /**
     * 修改文章
     * @param articleBo 文章对象
     * @return
     */
    public R<String> update(ArticleBo articleBo) {

        articleProducer.update(articleBo);
        return R.ok("修改成功");
    }

    /**
     * 删除文章
     * @param id 文章id
     * @return
     */
    public R<String> delete(Long id) {

        articleProducer.deleteById(id);
        return R.ok("删除成功");
    }


}

