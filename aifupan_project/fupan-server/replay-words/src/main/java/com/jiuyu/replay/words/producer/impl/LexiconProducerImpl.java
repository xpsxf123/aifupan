package com.jiuyu.replay.words.producer.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.common.utils.Query;
import com.jiuyu.replay.common.utils.SnowflakeManager;
import com.jiuyu.replay.words.bo.LexiconBo;
import com.jiuyu.replay.words.bo.LexiconListBo;
import com.jiuyu.replay.words.bo.LexiconWordListBo;
import com.jiuyu.replay.words.entity.LexiconEntity;
import com.jiuyu.replay.words.entity.LexiconWordEntity;
import com.jiuyu.replay.words.entity.SensitiveWordsClientEntity;
import com.jiuyu.replay.words.entity.TradeEntity;
import com.jiuyu.replay.words.producer.LexiconProducer;
import com.jiuyu.replay.words.repository.service.LexiconService;
import com.jiuyu.replay.words.repository.service.LexiconWordService;
import com.jiuyu.replay.words.repository.service.SensitiveWordsClientService;
import com.jiuyu.replay.words.repository.service.TradeService;
import com.jiuyu.replay.words.vo.LexiconInfoVo;
import com.jiuyu.replay.words.vo.LexiconListVo;
import com.jiuyu.replay.words.vo.SensitiveWordsClientListVo;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;


/**
 * 词库
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-10-06 16:34:13
 */
@Service
public class LexiconProducerImpl implements LexiconProducer {

    @Resource
    private LexiconService lexiconService;
    @Resource
    private SensitiveWordsClientService sensitiveWordsClientService;
    @Resource
    private LexiconWordService lexiconWordService;
    @Resource
    private TradeService tradeService;


    @Override
    public PageUtils<LexiconListVo> queryPage(LexiconListBo lexiconListBo) {
        QueryWrapper<LexiconEntity> wrapper = new QueryWrapper<>();
        if(!StringUtils.isEmpty(lexiconListBo.getKeyword())){
            wrapper.like("name", lexiconListBo.getKeyword());
        }
        if(!StringUtils.isEmpty(lexiconListBo.getUserId())){
            wrapper.eq("user_id", lexiconListBo.getUserId());
        }

        IPage<LexiconEntity> iPage = lexiconService.page(new Query<LexiconEntity>().getPage(lexiconListBo.getPage(), lexiconListBo.getLimit()), wrapper);

        PageUtils<LexiconListVo> pageUtils = new PageUtils<>(lexiconListBo.getPage(), lexiconListBo.getLimit(), iPage);

        List<LexiconEntity> records = iPage.getRecords();
        if(records != null && records.size() > 0) {

            // 获取词库的词语
            List<Long> lexiconIds = records.stream().map(LexiconEntity::getId).toList();
            List<LexiconWordEntity> lexiconWordEntities = this.lexiconWordService.list(new QueryWrapper<LexiconWordEntity>().in("lexicon_id", lexiconIds));
            
            // 获取词库的行业
            List<Long> tradeIds = records.stream().map(LexiconEntity::getTradeId).toList();
            List<TradeEntity> tradeEntities = this.tradeService.listByIds(tradeIds);

            List<LexiconListVo> vos = records.stream().map(item -> {
                LexiconListVo lexiconVo = new LexiconListVo();
                BeanUtils.copyProperties(item, lexiconVo);
                // 封装行业名称
                for (TradeEntity tradeEntity : tradeEntities) {
                    if(tradeEntity.getId().equals(lexiconVo.getTradeId())) {
                        lexiconVo.setTradeName(tradeEntity.getName());
                        break;
                    }
                }
                // 封装词库的词语数量
                if(lexiconWordEntities != null && lexiconWordEntities.size() > 0) {
                    for (LexiconWordEntity lexiconWordEntity : lexiconWordEntities) {
                        if(lexiconWordEntity.getLexiconId().equals(lexiconVo.getId())) {
                            if(lexiconWordEntity.getWordsType() == 0) {
                                lexiconVo.setSensitiveNum(lexiconVo.getSensitiveNum() + 1);
                            }else if(lexiconWordEntity.getWordsType() == 1) {
                                lexiconVo.setCruxNum(lexiconVo.getCruxNum() + 1);
                            }else if(lexiconWordEntity.getWordsType() == 2) {
                                lexiconVo.setWhiteNum(lexiconVo.getWhiteNum() + 1);
                            }
                        }
                    }
                }
                return lexiconVo;
            }).collect(Collectors.toList());

            pageUtils.setList(vos);
        }

        return pageUtils;
    }

    @Override
    public LexiconInfoVo info(Long id) {

        LexiconEntity lexiconEntity = lexiconService.getById(id);
        if(lexiconEntity != null) {
            LexiconInfoVo lexiconInfoVo = new LexiconInfoVo();
            BeanUtils.copyProperties(lexiconEntity, lexiconInfoVo);
            return lexiconInfoVo;
        }

        return null;
    }

    /**
     * 新增词库
     * @param lexiconBo 词库对象
     * @return
     */
    @Override
    public LexiconInfoVo save(LexiconBo lexiconBo) {

         LexiconEntity lexiconEntity = new LexiconEntity();
         BeanUtils.copyProperties(lexiconBo, lexiconEntity);
         lexiconEntity.setId(SnowflakeManager.nextValue());
         lexiconEntity.setCreateDate(new Date());
         lexiconEntity.setUpdateDate(new Date());

         lexiconService.save(lexiconEntity);

         LexiconInfoVo lexiconInfoVo = new LexiconInfoVo();
         BeanUtils.copyProperties(lexiconEntity, lexiconInfoVo);

         return lexiconInfoVo;
     }

    /**
     * 修改词库
     * @param lexiconBo 词库对象
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void update(LexiconBo lexiconBo) {

        LexiconEntity lexiconEntity = new LexiconEntity();
        BeanUtils.copyProperties(lexiconBo, lexiconEntity);
        lexiconEntity.setUpdateDate(new Date());

        lexiconService.updateById(lexiconEntity);

        if(!lexiconBo.getTradeId().equals(lexiconBo.getOldTradeId())) {
            // 修改关联词语的行业
            List<LexiconWordEntity> lexiconWordEntities = this.lexiconWordService.list(new QueryWrapper<LexiconWordEntity>().eq("lexicon_id", lexiconBo.getId()).select("word_id"));
            if(lexiconWordEntities != null && lexiconWordEntities.size() > 0) {
                List<Long> wordIds = lexiconWordEntities.stream().map(LexiconWordEntity::getWordId).toList();
                List<SensitiveWordsClientEntity> sensitiveWordsClientEntities = this.sensitiveWordsClientService.listByIds(wordIds);
                if(sensitiveWordsClientEntities != null && sensitiveWordsClientEntities.size() > 0) {
                    for (SensitiveWordsClientEntity sensitiveWordsClientEntity : sensitiveWordsClientEntities) {
                        sensitiveWordsClientEntity.setTradeId(lexiconBo.getTradeId());
                        sensitiveWordsClientEntity.setUpdateDate(new Date());
                    }
                    this.sensitiveWordsClientService.updateBatchById(sensitiveWordsClientEntities);
                }
            }
        }
    }

    /**
     * 删除词库
     * @param id 词库id
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void deleteById(Long id) {
        // 删除词库
        lexiconService.removeById(id);

        // 删除词库关联的词语
        List<LexiconWordEntity> lexiconWordEntities = this.lexiconWordService.list(new QueryWrapper<LexiconWordEntity>().eq("lexicon_id", id).select("word_id"));
        if(lexiconWordEntities != null && lexiconWordEntities.size() > 0) {
            List<Long> wordIds = lexiconWordEntities.stream().map(LexiconWordEntity::getWordId).toList();
            this.sensitiveWordsClientService.remove(new QueryWrapper<SensitiveWordsClientEntity>().in("id", wordIds));
        }
    }

    @Override
    public PageUtils<SensitiveWordsClientListVo> getWordsList(LexiconWordListBo lexiconWordListBo) {

        QueryWrapper<SensitiveWordsClientEntity> wrapper = new QueryWrapper<>();
        if(!StringUtils.isEmpty(lexiconWordListBo.getName())) {
            wrapper.like("name", lexiconWordListBo.getName());
        }
        if(!StringUtils.isEmpty(lexiconWordListBo.getLexiconId())) {
            List<LexiconWordEntity> lexiconWordEntities = this.lexiconWordService.list(new QueryWrapper<LexiconWordEntity>().eq("lexicon_id", lexiconWordListBo.getLexiconId()));
            if(lexiconWordEntities != null && lexiconWordEntities.size() > 0) {
                List<Long> wordIds = lexiconWordEntities.stream().map(LexiconWordEntity::getWordId).toList();
                wrapper.in("id", wordIds);
            }else {
                wrapper.eq("id", 0);
            }
        }
        if(!StringUtils.isEmpty(lexiconWordListBo.getWordType())) {
            wrapper.eq("words_type", lexiconWordListBo.getWordType());
        }
        if(!StringUtils.isEmpty(lexiconWordListBo.getStartTime())) {
            String startTime = lexiconWordListBo.getStartTime() + " 00:00:00";
            wrapper.ge("create_date", startTime);
        }
        if(!StringUtils.isEmpty(lexiconWordListBo.getEndTime())) {
            String endTime = lexiconWordListBo.getEndTime() + " 23:59:59";
            wrapper.le("create_date", endTime);
        }
        if (lexiconWordListBo.getCruxTypeIds() != null && lexiconWordListBo.getCruxTypeIds().size() > 0) {
            wrapper.in("crux_type_id", lexiconWordListBo.getCruxTypeIds());
        }

        IPage<SensitiveWordsClientEntity> iPage = sensitiveWordsClientService.page(new Query<SensitiveWordsClientEntity>().getPage(lexiconWordListBo.getPage(), lexiconWordListBo.getLimit()), wrapper);

        PageUtils<SensitiveWordsClientListVo> pageUtils = new PageUtils<>(lexiconWordListBo.getPage(), lexiconWordListBo.getLimit(), iPage);

        List<SensitiveWordsClientEntity> records = iPage.getRecords();

        if(records != null && records.size() > 0) {

            List<SensitiveWordsClientListVo> sensitiveWordsListVos = records.stream().map(item -> {
                SensitiveWordsClientListVo sensitiveWordsListVo = new SensitiveWordsClientListVo();
                BeanUtils.copyProperties(item, sensitiveWordsListVo);
                return sensitiveWordsListVo;
            }).toList();

            pageUtils.setList(sensitiveWordsListVos);

        }

        return pageUtils;
    }

    @Override
    public LexiconInfoVo getByUserIdAndTradeId(Long userId, Long tradeId) {
        LexiconEntity lexiconEntity = this.lexiconService.getOne(new QueryWrapper<LexiconEntity>().eq("user_id", userId).eq("trade_id", tradeId));
        if(lexiconEntity != null) {
            LexiconInfoVo lexiconInfoVo = new LexiconInfoVo();
            BeanUtils.copyProperties(lexiconEntity, lexiconInfoVo);
            return lexiconInfoVo;
        }
        return null;
    }


}

