package com.jiuyu.replay.common.producer.impl;

import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.common.utils.Query;
import com.jiuyu.replay.common.utils.SnowflakeManager;

import com.jiuyu.replay.common.vo.ArticleListVo;
import com.jiuyu.replay.common.vo.ArticleInfoVo;
import com.jiuyu.replay.common.bo.ArticleBo;
import com.jiuyu.replay.common.bo.ArticleListBo;
import com.jiuyu.replay.common.repository.service.ArticleService;
import com.jiuyu.replay.common.entity.ArticleEntity;
import com.jiuyu.replay.common.producer.ArticleProducer;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.beans.BeanUtils;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;


/**
 * 文章
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-08-23 20:39:45
 */
@Service
public class ArticleProducerImpl implements ArticleProducer {

    @Resource
    private ArticleService articleService;


    @Override
    public PageUtils<ArticleListVo> queryPage(ArticleListBo articleListBo) {
        QueryWrapper<ArticleEntity> wrapper = new QueryWrapper<>();
        if(!StringUtils.isEmpty(articleListBo.getKeyword())){
            wrapper.like("title", articleListBo.getKeyword());
        }
        if(!StringUtils.isEmpty(articleListBo.getType())){
            wrapper.eq("type", articleListBo.getType());
        }
        wrapper.orderByDesc("id");

        IPage<ArticleEntity> iPage = articleService.page(new Query<ArticleEntity>().getPage(articleListBo.getPage(), articleListBo.getLimit()), wrapper);

        PageUtils<ArticleListVo> pageUtils = new PageUtils<>(articleListBo.getPage(), articleListBo.getLimit(), iPage);

        List<ArticleEntity> records = iPage.getRecords();
        if(records != null && records.size() > 0) {
            List<ArticleListVo> vos = records.stream().map(item -> {
                ArticleListVo articleVo = new ArticleListVo();
                BeanUtils.copyProperties(item, articleVo);
                return articleVo;
            }).collect(Collectors.toList());

            pageUtils.setList(vos);
        }

        return pageUtils;
    }

    @Override
    public ArticleInfoVo info(Long id) {

        ArticleEntity articleEntity = articleService.getById(id);
        if(articleEntity != null) {
            ArticleInfoVo articleInfoVo = new ArticleInfoVo();
            BeanUtils.copyProperties(articleEntity, articleInfoVo);
            return articleInfoVo;
        }

        return null;
    }

    /**
     * 新增文章
     * @param articleBo 文章对象
     * @return
     */
     public ArticleInfoVo save(ArticleBo articleBo) {

         ArticleEntity articleEntity = new ArticleEntity();
         BeanUtils.copyProperties(articleBo, articleEntity);
         articleEntity.setId(SnowflakeManager.nextValue());
         articleEntity.setCreateDate(new Date());
         articleEntity.setUpdateDate(new Date());

         articleService.save(articleEntity);

         ArticleInfoVo articleInfoVo = new ArticleInfoVo();
         BeanUtils.copyProperties(articleEntity, articleInfoVo);

         return articleInfoVo;
     }

    /**
     * 修改文章
     * @param articleBo 文章对象
     * @return
     */
    public void update(ArticleBo articleBo) {

        ArticleEntity articleEntity = new ArticleEntity();
        BeanUtils.copyProperties(articleBo, articleEntity);
        articleEntity.setUpdateDate(new Date());

        articleService.updateById(articleEntity);
    }

    /**
     * 删除文章
     * @param id 文章id
     * @return
     */
    public void deleteById(Long id) {

        articleService.removeById(id);
    }


}

