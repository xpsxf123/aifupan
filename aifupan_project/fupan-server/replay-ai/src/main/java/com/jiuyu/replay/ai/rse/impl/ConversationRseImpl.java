package com.jiuyu.replay.ai.rse.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjectUtil;
import com.jiuyu.replay.ai.bo.UpdateCorrectStatusBo;
import com.jiuyu.replay.ai.bo.UpdateHtmlStatusBo;
import com.jiuyu.replay.ai.bo.UpdateLikesStatusBo;
import com.jiuyu.replay.ai.bo.conversationByCueWordsIdsBo;
import com.jiuyu.replay.ai.entity.ConversationEntity;
import com.jiuyu.replay.ai.repository.service.ConversationService;
import com.jiuyu.replay.ai.rse.ConversationRse;
import com.jiuyu.replay.common.constant.AiEnums;
import com.jiuyu.replay.common.repository.service.SystemKvService;
import com.jiuyu.replay.generic.bo.ai.ConversationBo;
import com.jiuyu.replay.generic.bo.ai.ConversationListBo;
import com.jiuyu.replay.generic.feign.power.UserFeign;
import com.jiuyu.replay.generic.utils.ResultUtil;
import com.jiuyu.replay.generic.vo.ai.ConversationPage;
import com.jiuyu.replay.generic.vo.ai.ConversationVo;
import com.jiuyu.replay.generic.vo.power.UserCacheVo;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

/**
 * @author ：lujie
 * @description：
 * @date ：2025/5/22 下午5:33
 */
@Slf4j
@Service
@AllArgsConstructor
public class ConversationRseImpl implements ConversationRse {

    private final ConversationService conversationService;
    private final MongoTemplate mongoTemplate;
    private final UserFeign userFeign;
    private final SystemKvService systemKvService;

    @Override
    public List<ConversationVo> saveAll(List<ConversationBo> dataList) {

        if (ObjectUtil.isNotEmpty(dataList)) {
            List<ConversationEntity> list = BeanUtil.copyToList(dataList, ConversationEntity.class);
            long millis = System.currentTimeMillis();
            list.forEach(item -> {
                item.setHtmlStatus(AiEnums.htmlStatus.WAITING.getCode());
                if (ObjectUtil.isEmpty(item.getCreateDate())) {
                    item.setCreateTime(millis);
                    item.setCreateDate(DateUtil.format(new Date(millis), "yyyy-MM-dd HH:mm:ss"));
                } else {
                    item.setCreateTime(DateUtil.parse(item.getCreateDate()).getTime());
                }
            });

            List<ConversationEntity> conversationEntities = conversationService.saveAll(list);
            return BeanUtil.copyToList(conversationEntities, ConversationVo.class);
        }
        return new ArrayList<>();
    }

    @Override
    public ConversationPage<ConversationVo> conversationPage(ConversationListBo listBo) {
        UserCacheVo user = ResultUtil.getUserResult(userFeign.getLocalUser());
        ConversationEntity entity = new ConversationEntity();
        entity.setSourceId(listBo.getSourceId());
        entity.setSourceType(listBo.getSourceType());
        entity.setUserId(user.getId());
        entity.setTenantId(user.getActiveTenantId());
        entity.setAskType(listBo.getType());
        ConversationPage<ConversationVo> result = new ConversationPage<>();
        result.setList(new ArrayList<>());
        result.setExistPreviousPage(false);
        Page<ConversationEntity> page = conversationService.page(entity, listBo.getPage(), listBo.getLimit());
        if (ObjectUtil.isNotEmpty(page.getContent())) {
            List<ConversationEntity> content = page.getContent();
            List<ConversationVo> list = BeanUtil.copyToList(content, ConversationVo.class);
            list.sort(Comparator.comparing(ConversationVo::getCreateTime).thenComparing(ConversationVo::getType));
            result.setList(list);
            result.setExistPreviousPage(page.hasNext());
        }

        return result;
    }

    @Override
    public List<ConversationVo> listByIds(List<String> ids) {
        List<ConversationEntity> listedByIds = conversationService.listByIds(ids);
        if (ObjectUtil.isNotEmpty(listedByIds)) {
            return BeanUtil.copyToList(listedByIds, ConversationVo.class);
        }
        return new ArrayList<>();
    }

    @Override
    public void updateLikesStatus(UpdateLikesStatusBo updateLikesStatusBo) {
        conversationService.updateLikesStatus(updateLikesStatusBo);
    }

    @Override
    public List<Long> existsCueWords(String sourceId, Integer sourceType, Long userId, Long tenantId, Integer cueType, List<Long> cueWordsIds) {
        // 构建查询条件
        Query query = new Query();
        query.addCriteria(Criteria.where("sourceId").is(sourceId)
                .and("sourceType").is(sourceType)
                .and("userId").is(userId)
                .and("tenantId").is(tenantId)
                .and("askType").is(cueType)
                .and("cueWordsId").in(cueWordsIds));
        // 只获取 cueWordsId 字段
        query.fields().include("cueWordsId");
        // 判断cueWordsIds中是否存在
        List<ConversationEntity> conversationEntities = mongoTemplate.find(query, ConversationEntity.class);

        if (ObjectUtil.isNotEmpty(conversationEntities)) {
            return conversationEntities.stream().map(ConversationEntity::getCueWordsId).distinct().toList();
        }

        return new ArrayList<>();
    }

    @Override
    public List<ConversationVo> conversationByCueWordsIds(conversationByCueWordsIdsBo bo) {
        Criteria criteria = Criteria.where("sourceId").is(bo.getVideoId())
                .and("sourceType").is(0)
                .and("userId").is(bo.getUserId())
                .and("tenantId").is(bo.getTenantId())
                .and("cueWordsId").in(bo.getCueWordsIds());
        Aggregation agg = Aggregation.newAggregation(
                Aggregation.match(criteria),
                Aggregation.sort(Sort.by(Sort.Direction.DESC, "createTime")),
                Aggregation.group("cueWordsId", "type")
                        .first("type").as("type")
                        .first("content").as("content")
                        .first("askType").as("askType")
                        .first("completionId").as("completionId")
                        .first("cueWordsId").as("cueWordsId")
                        .first("qaCode").as("qaCode")
                        .first("contextId").as("contextId")
                        .first("type").as("type")
                        .first("giveStatuc").as("giveStatuc")
                        .first("createDate").as("createDate")
                        .first("createTime").as("createTime")
        );
        AggregationResults<ConversationEntity> aggregate = mongoTemplate.aggregate(agg, "replay_ai_conversation", ConversationEntity.class);
        List<ConversationEntity> mappedResults = aggregate.getMappedResults();
        if (ObjectUtil.isNotEmpty(mappedResults)) {
            mappedResults = mappedResults.stream()
                    .sorted(Comparator.comparing(ConversationEntity::getCreateTime).reversed().thenComparing(ConversationEntity::getType))
                    .toList()
            ;
            return BeanUtil.copyToList(mappedResults, ConversationVo.class);
        }
        return new ArrayList<>();
    }


    @Override
    public Boolean isExist(ConversationBo bo) {
        Query query = Query.query(
                Criteria.where("sourceId").is(bo.getSourceId())
                        .and("sourceType").is(bo.getSourceType())
                        .and("userId").is(bo.getUserId())
                        .and("tenantId").is(bo.getTenantId())
                        .and("askType").is(bo.getAskType())
        );

        return mongoTemplate.exists(query, ConversationEntity.class);
    }

    @Override
    public List<ConversationEntity> listByQaCodes(List<String> qaCodes) {
        Query query = Query.query(
                Criteria.where("qaCode").in(qaCodes)
        );
        return mongoTemplate.find(query, ConversationEntity.class);
    }

    @Override
    public ConversationVo getById(String id) {
        Query query = Query.query(
                Criteria.where("_id").is(id)
        );
        ConversationEntity one = mongoTemplate.findOne(query, ConversationEntity.class);
        if (one != null) {
            return BeanUtil.copyProperties(one, ConversationVo.class);
        }
        return null;
    }

    @Override
    public void updateHtmlStatus(UpdateHtmlStatusBo updateHtmlStatusBo) {
        conversationService.updateHtmlStatus(updateHtmlStatusBo);
    }

    @Override
    public List<ConversationVo> getHtmlStatus(List<String> ids) {
        Query query = Query.query(
                Criteria.where("_id").in(ids)
        );
        // 只查询HTML相关字段和ID
        query.fields()
                .include("_id")
                .include("htmlType")
                .include("htmlStatus")
                .include("htmlCreateDate")
                .include("htmlSavePath")
                .include("htmlCreateError");

        List<ConversationEntity> one = mongoTemplate.find(query, ConversationEntity.class);
        return BeanUtil.copyToList(one, ConversationVo.class);
    }

    @Override
    public void updateUserHtmlFail(Long userId, Long tenantId) {
        // 构建查询条件
        Query query = new Query();
        query.addCriteria(
                Criteria.where("userId").is(userId)
                        .and("tenantId").is(tenantId)
                        .and("htmlStatus").is(AiEnums.htmlStatus.GENERATING.getCode())
                        .and("htmlType").is(AiEnums.htmlType.CLIENT.getCode())
        );

        // 构建更新内容
        Update update = new Update();
        update.set("htmlStatus", AiEnums.htmlStatus.FAIL.getCode())
                .set("htmlCreateError", "客户端关闭，生成失败");

        // 执行批量更新
        mongoTemplate.updateMulti(query, update, ConversationEntity.class);
    }

    @Override
    public void updateHtmlStatus(List<String> ids, Integer status, String errorMsg) {
        // 构建查询条件
        Query query = new Query();
        query.addCriteria(
                Criteria.where("_id").is(ids)
        );

        // 构建更新内容
        Update update = new Update();
        update.set("htmlStatus", status);
        if (ObjectUtil.equals(AiEnums.htmlStatus.FAIL.getCode(), status) && ObjectUtil.isNotEmpty(errorMsg)) {
            update.set("htmlCreateError", errorMsg);
        }

        // 执行批量更新
        mongoTemplate.updateMulti(query, update, ConversationEntity.class);
    }

    @Override
    public void updateCorrectStatus(UpdateCorrectStatusBo updateCorrectStatusBo) {
        conversationService.updateCorrectStatus(updateCorrectStatusBo);
    }

    @Override
    public void updateCorrectStatus(List<String> ids, Integer status, String errorMsg) {
        // 构建查询条件
        Query query = new Query();
        query.addCriteria(
                Criteria.where("_id").in(ids)
        );

        // 构建更新内容
        Update update = new Update();
        update.set("aiCorrectStatus", status);
        if (ObjectUtil.equals(AiEnums.correctStatus.FAIL.getCode(), status) && ObjectUtil.isNotEmpty(errorMsg)) {
            update.set("aiCorrectError", errorMsg);
        }

        // 执行批量更新
        mongoTemplate.updateMulti(query, update, ConversationEntity.class);
    }

    @Override
    public List<ConversationVo> getCorrectStatus(List<String> ids) {
        Query query = Query.query(
                Criteria.where("_id").in(ids)
        );
        // 只查询纠正相关字段和ID
        query.fields()
                .include("_id")
                .include("aiCorrectStatus")
                .include("aiCorrectType")
                .include("aiCorrectError")
                .include("content")
        ;

        List<ConversationEntity> entities = mongoTemplate.find(query, ConversationEntity.class);
        return BeanUtil.copyToList(entities, ConversationVo.class);
    }

    @Override
    public String getDataDiagnosisContent(String sourceId, Long userId, Long tenantId, Long cueWordsId) {
        Query query = Query.query(
                Criteria.where("sourceId").is(sourceId)
                        .and("sourceType").is(0)
                        .and("userId").is(userId)
                        .and("tenantId").is(tenantId)
                        .and("cueWordsId").is(cueWordsId)
                        .and("type").is("Q")
        );
        query.with(Sort.by(Sort.Direction.DESC, "createTime"));
        query.limit(1);
        query.fields().include("content");
        ConversationEntity entity = mongoTemplate.findOne(query, ConversationEntity.class);
        return entity != null ? entity.getContent() : null;
    }

    @Override
    public void updateUserCorrectFail(Long userId, Long tenantId) {
        // 构建查询条件
        Query query = new Query();
        query.addCriteria(
                Criteria.where("userId").is(userId)
                        .and("tenantId").is(tenantId)
                        .and("aiCorrectStatus").is(AiEnums.correctStatus.CORRECTING.getCode())
                        .and("aiCorrectType").is(AiEnums.correctType.CLIENT.getCode())
        );

        // 构建更新内容
        Update update = new Update();
        update.set("aiCorrectStatus", AiEnums.correctStatus.FAIL.getCode())
                .set("aiCorrectError", "客户端关闭，纠正失败");

        // 执行批量更新
        mongoTemplate.updateMulti(query, update, ConversationEntity.class);
    }
}
