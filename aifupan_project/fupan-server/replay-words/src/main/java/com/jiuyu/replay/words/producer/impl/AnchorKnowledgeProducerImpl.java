package com.jiuyu.replay.words.producer.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.jiuyu.replay.common.utils.Query;
import com.jiuyu.replay.common.constant.WordsEnum;
import com.jiuyu.replay.common.utils.SnowflakeManager;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.words.AnchorUrlInfoVo;
import com.jiuyu.replay.words.bo.AnchorKnowledgeListBo;
import com.jiuyu.replay.words.bo.AnchorKnowledgeSaveBo;
import com.jiuyu.replay.words.entity.AnchorKnowledgeEntity;
import com.jiuyu.replay.words.producer.AnchorKnowledgeProducer;
import com.jiuyu.replay.words.producer.AnchorUrlProducer;
import com.jiuyu.replay.words.repository.service.AnchorKnowledgeService;
import com.jiuyu.replay.words.vo.AnchorKnowledgeListVo;
import com.jiuyu.replay.words.vo.AnchorKnowledgeVo;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 主播级别知识库 Producer 实现
 *
 * @author jy
 * @date 2026-06-29
 */
@Service
@Slf4j
public class AnchorKnowledgeProducerImpl implements AnchorKnowledgeProducer {

    @Resource
    private AnchorKnowledgeService anchorKnowledgeService;
    @Resource
    private AnchorUrlProducer anchorUrlProducer;


    @Override
    public PageUtils<AnchorKnowledgeListVo> queryPage(AnchorKnowledgeListBo listBo) {
        QueryWrapper<AnchorKnowledgeEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", listBo.getUserId());
        wrapper.eq("is_deleted", 0);
        wrapper.orderByDesc("update_date");

        IPage<AnchorKnowledgeEntity> iPage = anchorKnowledgeService.page(
                new Query<AnchorKnowledgeEntity>().getPage(listBo.getPage(), listBo.getLimit()), wrapper);

        PageUtils<AnchorKnowledgeListVo> pageUtils = new PageUtils<>(listBo.getPage(), listBo.getLimit(), iPage);

        List<AnchorKnowledgeEntity> records = iPage.getRecords();
        if (CollUtil.isNotEmpty(records)) {
            // 按 (userId, secUid) 分组聚合
            Map<String, List<AnchorKnowledgeEntity>> groupMap = records.stream()
                    .collect(Collectors.groupingBy(e -> e.getUserId() + "_" + e.getSecUid()));

            List<String> secUids = records.stream()
                    .map(AnchorKnowledgeEntity::getSecUid).distinct().collect(Collectors.toList());
            Map<String, String> anchorNameMap = resolveAnchorNames(secUids);

            // 取每组最新 updateDate 作为列表项时间
            List<AnchorKnowledgeListVo> vos = groupMap.entrySet().stream().map(entry -> {
                List<AnchorKnowledgeEntity> rows = entry.getValue();
                AnchorKnowledgeListVo vo = new AnchorKnowledgeListVo();
                vo.setSecUid(rows.get(0).getSecUid());
                vo.setUserId(rows.get(0).getUserId());
                vo.setAnchorName(anchorNameMap.getOrDefault(rows.get(0).getSecUid(), ""));
                Date latestUpdate = rows.stream()
                        .map(AnchorKnowledgeEntity::getUpdateDate)
                        .filter(Objects::nonNull)
                        .max(Date::compareTo).orElse(null);
                Date earliestCreate = rows.stream()
                        .map(AnchorKnowledgeEntity::getCreateDate)
                        .filter(Objects::nonNull)
                        .min(Date::compareTo).orElse(null);
                vo.setUpdateDate(latestUpdate);
                vo.setCreateDate(earliestCreate);
                for (AnchorKnowledgeEntity row : rows) {
                    int kt = row.getKnowledgeType();
                    if (kt == WordsEnum.knowledgeType.OPERATION.getCode()) {
                        vo.setOperationContent(row.getContent());
                    } else if (kt == WordsEnum.knowledgeType.SENSITIVE.getCode()) {
                        vo.setSensitiveContent(row.getContent());
                    } else if (kt == WordsEnum.knowledgeType.HEALTH_SCORE.getCode()) {
                        vo.setHealthScore(row.getContent());
                    }
                }
                return vo;
            }).collect(Collectors.toList());

            vos.sort((a, b) -> {
                if (a.getUpdateDate() == null && b.getUpdateDate() == null) return 0;
                if (a.getUpdateDate() == null) return 1;
                if (b.getUpdateDate() == null) return -1;
                return b.getUpdateDate().compareTo(a.getUpdateDate());
            });
            pageUtils.setList(vos);
            pageUtils.setTotalCount(vos.size());
        }

        return pageUtils;
    }

    @Override
    public AnchorKnowledgeVo getByUserAndSecUid(Long userId, String secUid) {
        List<AnchorKnowledgeEntity> rows = anchorKnowledgeService.list(
                new QueryWrapper<AnchorKnowledgeEntity>()
                        .eq("user_id", userId)
                        .eq("sec_uid", secUid)
                        .eq("is_deleted", 0)
        );

        if (CollUtil.isEmpty(rows)) return null;

        AnchorKnowledgeVo vo = new AnchorKnowledgeVo();
        vo.setUserId(userId);
        vo.setSecUid(secUid);
        Date latestUpdate = null;
        Date earliestCreate = null;
        for (AnchorKnowledgeEntity row : rows) {
            int kt = row.getKnowledgeType();
            if (kt == WordsEnum.knowledgeType.OPERATION.getCode()) {
                vo.setOperationContent(row.getContent());
            } else if (kt == WordsEnum.knowledgeType.SENSITIVE.getCode()) {
                vo.setSensitiveContent(row.getContent());
            } else if (kt == WordsEnum.knowledgeType.HEALTH_SCORE.getCode()) {
                vo.setHealthScore(row.getContent());
            }
            if (row.getUpdateDate() != null && (latestUpdate == null || row.getUpdateDate().after(latestUpdate))) {
                latestUpdate = row.getUpdateDate();
            }
            if (row.getCreateDate() != null && (earliestCreate == null || row.getCreateDate().before(earliestCreate))) {
                earliestCreate = row.getCreateDate();
            }
        }
        vo.setUpdateDate(latestUpdate);
        vo.setCreateDate(earliestCreate);
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveOrUpdate(AnchorKnowledgeSaveBo bo) {
        Long userId = bo.getUserId();
        String secUid = bo.getSecUid();

        upsertByType(userId, secUid, WordsEnum.knowledgeType.OPERATION.getCode(), bo.getOperationContent());
        upsertByType(userId, secUid, WordsEnum.knowledgeType.SENSITIVE.getCode(), bo.getSensitiveContent());
        upsertByType(userId, secUid, WordsEnum.knowledgeType.HEALTH_SCORE.getCode(), bo.getHealthScore());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteBySecUid(Long userId, String secUid) {
        List<AnchorKnowledgeEntity> rows = anchorKnowledgeService.list(
                new QueryWrapper<AnchorKnowledgeEntity>()
                        .eq("user_id", userId)
                        .eq("sec_uid", secUid)
                        .eq("is_deleted", 0)
        );
        if (CollUtil.isEmpty(rows)) return;

        for (AnchorKnowledgeEntity row : rows) {
            row.setIsDeleted(1);
            row.setUpdateDate(new Date());
            anchorKnowledgeService.updateById(row);
        }
    }

    /**
     * 单类型 upsert
     */
    private void upsertByType(Long userId, String secUid, int knowledgeType, String content) {
        if (content == null) return;

        AnchorKnowledgeEntity exist = anchorKnowledgeService.getOne(
                new QueryWrapper<AnchorKnowledgeEntity>()
                        .eq("user_id", userId)
                        .eq("sec_uid", secUid)
                        .eq("knowledge_type", knowledgeType)
                        .eq("is_deleted", 0)
        );

        if (exist != null) {
            exist.setContent(content);
            exist.setUpdateDate(new Date());
            anchorKnowledgeService.updateById(exist);
        } else {
            AnchorKnowledgeEntity entity = new AnchorKnowledgeEntity();
            entity.setId(SnowflakeManager.nextValue());
            entity.setUserId(userId);
            entity.setSecUid(secUid);
            entity.setKnowledgeType(knowledgeType);
            entity.setContent(content);
            entity.setCreateDate(new Date());
            entity.setUpdateDate(new Date());
            entity.setIsDeleted(0);
            anchorKnowledgeService.save(entity);
        }
    }

    private Map<String, String> resolveAnchorNames(List<String> secUids) {
        try {
            List<AnchorUrlInfoVo> list = anchorUrlProducer.listBySecUids(secUids);
            if (CollUtil.isEmpty(list)) return Collections.emptyMap();
            return list.stream()
                    .filter(a -> a.getAnchorName() != null)
                    .collect(Collectors.toMap(AnchorUrlInfoVo::getSecUid, AnchorUrlInfoVo::getAnchorName, (a, b) -> a));
        } catch (Exception e) {
            log.warn("解析主播名称失败", e);
            return Collections.emptyMap();
        }
    }
}
