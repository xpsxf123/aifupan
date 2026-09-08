package com.jiuyu.replay.words.producer.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.common.utils.Query;
import com.jiuyu.replay.common.utils.SnowflakeManager;
import com.jiuyu.replay.words.bo.CruxWordsBatchBo;
import com.jiuyu.replay.words.bo.CruxWordsBo;
import com.jiuyu.replay.words.bo.CruxWordsListBo;
import com.jiuyu.replay.words.bo.WordsBatchItemBo;
import com.jiuyu.replay.words.entity.CruxWordsEntity;
import com.jiuyu.replay.words.producer.CruxWordsProducer;
import com.jiuyu.replay.words.repository.service.CruxWordsService;
import com.jiuyu.replay.words.repository.service.TradeService;
import com.jiuyu.replay.words.vo.CruxWordsInfoVo;
import com.jiuyu.replay.words.vo.CruxWordsListVo;
import com.jiuyu.replay.words.vo.CruxWordsVo;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;


/**
 * 关键词
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-06-20 16:20:06
 */
@Service
public class CruxWordsProducerImpl implements CruxWordsProducer {

    @Resource
    private CruxWordsService cruxWordsService;
    @Resource
    private TradeService tradeService;


    @Override
    public PageUtils<CruxWordsListVo> queryPage(CruxWordsListBo cruxWordsListBo) {
        QueryWrapper<CruxWordsEntity> wrapper = new QueryWrapper<>();
        if (!StringUtils.isEmpty(cruxWordsListBo.getKeyword())) {
            wrapper.like("name", cruxWordsListBo.getKeyword());
        }
        if (!StringUtils.isEmpty(cruxWordsListBo.getResourceType())) {
            wrapper.eq("resource_type", cruxWordsListBo.getResourceType());
        }
        if (!StringUtils.isEmpty(cruxWordsListBo.getType())) {
            wrapper.eq("type", cruxWordsListBo.getType());
        }
        if (!StringUtils.isEmpty(cruxWordsListBo.getPlatformType())) {
            wrapper.eq("platform_type", cruxWordsListBo.getPlatformType());
        }
        if (!StringUtils.isEmpty(cruxWordsListBo.getTradeId())) {
            wrapper.eq("trade_id", cruxWordsListBo.getTradeId());
        }
        if (!StringUtils.isEmpty(cruxWordsListBo.getGroupStr())) {
            wrapper.like("group_str", cruxWordsListBo.getGroupStr());
        }

        IPage<CruxWordsEntity> iPage = cruxWordsService.page(new Query<CruxWordsEntity>().getPage(cruxWordsListBo.getPage(), cruxWordsListBo.getLimit()), wrapper);

        PageUtils<CruxWordsListVo> pageUtils = new PageUtils<>(cruxWordsListBo.getPage(), cruxWordsListBo.getLimit(), iPage);

        List<CruxWordsEntity> records = iPage.getRecords();
        if (records != null && records.size() > 0) {

            List<CruxWordsListVo> vos = records.stream().map(item -> {
                CruxWordsListVo cruxWordsVo = new CruxWordsListVo();
                BeanUtils.copyProperties(item, cruxWordsVo);
                return cruxWordsVo;
            }).collect(Collectors.toList());

            pageUtils.setList(vos);
        }

        return pageUtils;
    }

    @Override
    public CruxWordsInfoVo info(Long id) {

        CruxWordsEntity cruxWordsEntity = cruxWordsService.getById(id);
        if (cruxWordsEntity != null) {
            CruxWordsInfoVo cruxWordsInfoVo = new CruxWordsInfoVo();
            BeanUtils.copyProperties(cruxWordsEntity, cruxWordsInfoVo);
            return cruxWordsInfoVo;
        }

        return null;
    }


    @Override
    public void save(CruxWordsBo cruxWordsBo, List<String> existList) {

        LinkedList<CruxWordsBo> cruxWordsBos = new LinkedList<>();
        cruxWordsBos.add(cruxWordsBo);
        if(cruxWordsBo.getChildren() != null && cruxWordsBo.getChildren().size() > 0) {
            if(existList == null) {
                // 没有已存在的词语，添加全部子词语
                cruxWordsBos.addAll(cruxWordsBo.getChildren());
            }else {
                for (CruxWordsBo child : cruxWordsBo.getChildren()) {
                    if(existList.contains(child.getName())) {
                        cruxWordsBos.add(child);
                    }
                }
            }
        }

        List<CruxWordsEntity> cruxWordsEntities = cruxWordsBos.stream().map(item -> {
            CruxWordsEntity cruxWordsEntity = new CruxWordsEntity();
            BeanUtils.copyProperties(cruxWordsBo, cruxWordsEntity);
            cruxWordsEntity.setId(SnowflakeManager.nextValue());
            cruxWordsEntity.setCreateDate(new Date());
            cruxWordsEntity.setUpdateDate(new Date());
            cruxWordsEntity.setName(item.getName());
            cruxWordsEntity.setRemarks(item.getRemarks());
            return cruxWordsEntity;
        }).toList();

        // 添加敏感词
        cruxWordsService.saveBatch(cruxWordsEntities);
    }

    /**
     * 修改关键词
     *
     * @param cruxWordsBo 关键词对象
     * @return
     */
    public void update(CruxWordsBo cruxWordsBo) {

        CruxWordsEntity cruxWordsEntity = new CruxWordsEntity();
        BeanUtils.copyProperties(cruxWordsBo, cruxWordsEntity);
        cruxWordsEntity.setUpdateDate(new Date());

        cruxWordsService.updateById(cruxWordsEntity);
    }

    /**
     * 删除关键词
     *
     * @param id 关键词id
     * @return
     */
    public void deleteById(Long id) {

        cruxWordsService.removeById(id);
    }

    @Override
    public List<CruxWordsVo> listByUidAndPidAndTid(Long userId, Integer platformType, Long tradeId) {
        QueryWrapper<CruxWordsEntity> wrapper = new QueryWrapper<>();
        wrapper.and(w -> {
            w.eq("resource_type", 0).or(w1 -> {
                w1.eq("resource_type", 1).eq("user_id", userId);
            });
        });
        wrapper.and(w -> {
            w.in("platform_type", 0, platformType);
        });
        wrapper.and(w -> {
            w.in("trade_id", 1, tradeId);
        });

        List<CruxWordsEntity> cruxWordsEntities = this.cruxWordsService.list(wrapper);

        if (cruxWordsEntities != null && cruxWordsEntities.size() > 0) {
            List<CruxWordsVo> cruxWordsVoList = cruxWordsEntities.stream().map(item -> {
                CruxWordsVo cruxWordsVo = new CruxWordsVo();
                BeanUtils.copyProperties(item, cruxWordsVo);
                return cruxWordsVo;
            }).toList();

            return cruxWordsVoList;
        }

        return null;
    }

    @Override
    public boolean exist(CruxWordsBo cruxWordsBo) {
        QueryWrapper<CruxWordsEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("name", cruxWordsBo.getName());
        wrapper.eq("trade_id", cruxWordsBo.getTradeId());
        wrapper.eq("resource_type", cruxWordsBo.getResourceType());
        wrapper.eq("type", cruxWordsBo.getType());

        CruxWordsEntity cruxWordsEntity = this.cruxWordsService.getOne(wrapper);

        return cruxWordsEntity != null;
    }

    @Override
    public void saveBatch(CruxWordsBatchBo cruxWordsBatchBo) {
        List<WordsBatchItemBo> wordsList = cruxWordsBatchBo.getWordsList();
        if (wordsList != null && wordsList.size() > 0) {
            List<CruxWordsEntity> cruxWordsEntities = wordsList.stream().map(item -> {
                CruxWordsEntity cruxWordsEntity = new CruxWordsEntity();
                BeanUtils.copyProperties(cruxWordsBatchBo, cruxWordsEntity);
                cruxWordsEntity.setId(SnowflakeManager.nextValue());
                cruxWordsEntity.setCreateDate(new Date());
                cruxWordsEntity.setUpdateDate(new Date());
                cruxWordsEntity.setName(item.getWords());
                List<String> similarWords = item.getSimilarWords();
                if (similarWords != null && similarWords.size() > 0) {
                    cruxWordsEntity.setSimilarWords(String.join("_", similarWords));
                }

                return cruxWordsEntity;
            }).filter(item -> !StringUtils.isEmpty(item.getName())).toList();

            if (cruxWordsEntities.size() > 0) {
                this.cruxWordsService.saveBatch(cruxWordsEntities);
            }

        }

    }

    @Override
    public List<CruxWordsVo> listRepeat(CruxWordsBatchBo cruxWordsBatchBo) {
        List<WordsBatchItemBo> wordsList = cruxWordsBatchBo.getWordsList();
        if (wordsList != null && wordsList.size() > 0) {
            List<String> cruxWords = wordsList.stream().map(WordsBatchItemBo::getWords).toList();
            QueryWrapper<CruxWordsEntity> wrapper = new QueryWrapper<>();

            wrapper.in("name", cruxWords);
            wrapper.eq("trade_id", cruxWordsBatchBo.getTradeId());
            wrapper.eq("resource_type", cruxWordsBatchBo.getResourceType());
            wrapper.eq("type", cruxWordsBatchBo.getType());
            wrapper.eq("platform_type", cruxWordsBatchBo.getPlatformType());

            List<CruxWordsEntity> cruxWordsEntities = this.cruxWordsService.list(wrapper);

            if (cruxWordsEntities != null && cruxWordsEntities.size() > 0) {
                List<CruxWordsVo> cruxWordsVos = cruxWordsEntities.stream().map(item -> {
                    CruxWordsVo cruxWordsVo = new CruxWordsVo();
                    BeanUtils.copyProperties(item, cruxWordsVo);
                    return cruxWordsVo;
                }).toList();

                return cruxWordsVos;
            }
        }
        return null;
    }

    @Override
    public List<String> existList(CruxWordsBo cruxWordsBo) {
        LinkedList<String> nameList = new LinkedList<>();
        nameList.add(cruxWordsBo.getName());
        if (cruxWordsBo.getChildren() != null && cruxWordsBo.getChildren().size() > 0) {
            for (CruxWordsBo child : cruxWordsBo.getChildren()) {
                nameList.add(child.getName());
            }
        }

        QueryWrapper<CruxWordsEntity> wrapper = new QueryWrapper<>();
        wrapper.in("name", nameList);
        wrapper.eq("trade_id", cruxWordsBo.getTradeId());
        wrapper.eq("resource_type", cruxWordsBo.getResourceType());
        wrapper.eq("type", cruxWordsBo.getType());

        List<CruxWordsEntity> cruxWordsEntities = this.cruxWordsService.list(wrapper);
        if (cruxWordsEntities != null && cruxWordsEntities.size() > 0) {
            return cruxWordsEntities.stream().map(CruxWordsEntity::getName).toList();
        }
        return null;
    }


}

