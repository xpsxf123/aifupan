package com.jiuyu.replay.words.producer.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.common.utils.Query;
import com.jiuyu.replay.common.utils.SnowflakeManager;
import com.jiuyu.replay.words.bo.LexiconWordBo;
import com.jiuyu.replay.words.bo.LexiconWordListBo;
import com.jiuyu.replay.words.entity.LexiconWordEntity;
import com.jiuyu.replay.words.producer.LexiconWordProducer;
import com.jiuyu.replay.words.repository.service.LexiconWordService;
import com.jiuyu.replay.words.vo.LexiconWordInfoVo;
import com.jiuyu.replay.words.vo.LexiconWordListVo;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;


/**
 * 词库-词语关联
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-10-08 10:41:43
 */
@Service
public class LexiconWordProducerImpl implements LexiconWordProducer {

    @Resource
    private LexiconWordService lexiconWordService;


    @Override
    public PageUtils<LexiconWordListVo> queryPage(LexiconWordListBo lexiconWordListBo) {
        QueryWrapper<LexiconWordEntity> wrapper = new QueryWrapper<>();

        IPage<LexiconWordEntity> iPage = lexiconWordService.page(new Query<LexiconWordEntity>().getPage(lexiconWordListBo.getPage(), lexiconWordListBo.getLimit()), wrapper);

        PageUtils<LexiconWordListVo> pageUtils = new PageUtils<>(lexiconWordListBo.getPage(), lexiconWordListBo.getLimit(), iPage);

        List<LexiconWordEntity> records = iPage.getRecords();
        if(records != null && records.size() > 0) {
            List<LexiconWordListVo> vos = records.stream().map(item -> {
                LexiconWordListVo lexiconWordVo = new LexiconWordListVo();
                BeanUtils.copyProperties(item, lexiconWordVo);
                return lexiconWordVo;
            }).collect(Collectors.toList());

            pageUtils.setList(vos);
        }

        return pageUtils;
    }

    @Override
    public LexiconWordInfoVo info(Long id) {

        LexiconWordEntity lexiconWordEntity = lexiconWordService.getById(id);
        if(lexiconWordEntity != null) {
            LexiconWordInfoVo lexiconWordInfoVo = new LexiconWordInfoVo();
            BeanUtils.copyProperties(lexiconWordEntity, lexiconWordInfoVo);
            return lexiconWordInfoVo;
        }

        return null;
    }

    /**
     * 新增词库-词语关联
     * @param lexiconWordBo 词库-词语关联对象
     * @return
     */
     public LexiconWordInfoVo save(LexiconWordBo lexiconWordBo) {

         LexiconWordEntity lexiconWordEntity = new LexiconWordEntity();
         BeanUtils.copyProperties(lexiconWordBo, lexiconWordEntity);
         lexiconWordEntity.setId(SnowflakeManager.nextValue());
         lexiconWordEntity.setCreateDate(new Date());
         lexiconWordEntity.setUpdateDate(new Date());

         lexiconWordService.save(lexiconWordEntity);

         LexiconWordInfoVo lexiconWordInfoVo = new LexiconWordInfoVo();
         BeanUtils.copyProperties(lexiconWordEntity, lexiconWordInfoVo);

         return lexiconWordInfoVo;
     }

    /**
     * 修改词库-词语关联
     * @param lexiconWordBo 词库-词语关联对象
     * @return
     */
    public void update(LexiconWordBo lexiconWordBo) {

        LexiconWordEntity lexiconWordEntity = new LexiconWordEntity();
        BeanUtils.copyProperties(lexiconWordBo, lexiconWordEntity);
        lexiconWordEntity.setUpdateDate(new Date());

        lexiconWordService.updateById(lexiconWordEntity);
    }

    /**
     * 删除词库-词语关联
     * @param id 词库-词语关联id
     * @return
     */
    public void deleteById(Long id) {

        lexiconWordService.removeById(id);
    }

    @Override
    public void deleteByWordId(Long wordId) {

        lexiconWordService.remove(new QueryWrapper<LexiconWordEntity>().eq("word_id", wordId));

    }


}

