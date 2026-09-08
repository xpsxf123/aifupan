package com.jiuyu.replay.ai.repository.service.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjectUtil;
import com.jiuyu.replay.ai.bo.UpdateCorrectStatusBo;
import com.jiuyu.replay.ai.bo.UpdateHtmlStatusBo;
import com.jiuyu.replay.ai.bo.UpdateLikesStatusBo;
import com.jiuyu.replay.ai.entity.ConversationEntity;
import com.jiuyu.replay.ai.repository.mongo.ConversationRepository;
import com.jiuyu.replay.ai.repository.service.ConversationService;
import com.jiuyu.replay.common.constant.AiEnums;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author ：lujie
 * @description：
 * @date ：2025/5/22 下午3:53
 */
@Service
@AllArgsConstructor
public class ConversationServiceImpl implements ConversationService {

    private final ConversationRepository conversationDao;
    private final MongoTemplate mongoTemplate;


    @Override
    public List<ConversationEntity> saveAll(List<ConversationEntity> list) {
        return conversationDao.saveAll(list);
    }

    /**
     * 分页查询
     *
     * @param conversationEntity 查询条件
     * @param page               当前页
     * @param limit              每页记录数
     * @return
     */
    @Override
    public Page<ConversationEntity> page(ConversationEntity conversationEntity, int page, int limit) {
        Sort by = Sort.by(Sort.Direction.DESC, "createTime").and(Sort.by(Sort.Direction.ASC, "type"));
        Page<ConversationEntity> all = conversationDao.findAll(Example.of(conversationEntity), PageRequest.of(page - 1, limit, by));
        if (ObjectUtil.isNotEmpty(all) && ObjectUtil.isNotEmpty(all.getContent())) {
            all.getContent().forEach(entity -> entity.setRealContent(null));
        }
        return all;
    }

    @Override
    public List<ConversationEntity> listByIds(List<String> ids) {
        List<ConversationEntity> allById = conversationDao.findAllById(ids);
        if (ObjectUtil.isNotEmpty(allById)){
            allById.forEach(entity -> entity.setRealContent(null));
            return allById;
        }
        return new ArrayList<>();
    }

    @Override
    public void updateLikesStatus(UpdateLikesStatusBo updateLikesStatusBo) {
        ConversationEntity probe = new ConversationEntity();
        probe.setId(updateLikesStatusBo.getId());
        ConversationEntity e = conversationDao.findOne(Example.of(probe)).orElse(null);
        if (e != null){
            e.setGiveStatuc(updateLikesStatusBo.getGiveStatuc());
            conversationDao.save(e);
        }
    }

    @Override
    public void updateHtmlStatus(UpdateHtmlStatusBo updateHtmlStatusBo) {
        Query query = Query.query(Criteria.where("_id").is(updateHtmlStatusBo.getId()));
        Update update = new Update();
        update.set("htmlStatus", updateHtmlStatusBo.getHtmlStatus());

        if (updateHtmlStatusBo.getHtmlStatus() == AiEnums.htmlStatus.GENERATING.getCode()) {
            // 状态为生成中（1），更新htmlCreateDate和htmlType
            update.set("htmlCreateDate", DateUtil.format(new Date(), "yyyy-MM-dd HH:mm:ss"));
            // 查询现有的htmlType，如果没有则设置默认值
            if (ObjectUtil.isNotEmpty(updateHtmlStatusBo.getHtmlType())) {
                update.set("htmlType", updateHtmlStatusBo.getHtmlType());
            }
        } else if (updateHtmlStatusBo.getHtmlStatus() == AiEnums.htmlStatus.SUCCESS.getCode() && ObjectUtil.isNotEmpty(updateHtmlStatusBo.getHtmlSavePath())) {
            // 状态为成功（2），更新htmlSavePath
            update.set("htmlSavePath", updateHtmlStatusBo.getHtmlSavePath());
        } else if (updateHtmlStatusBo.getHtmlStatus() == AiEnums.htmlStatus.FAIL.getCode() && ObjectUtil.isNotEmpty(updateHtmlStatusBo.getHtmlCreateError())) {
            // 状态为失败（3），更新htmlCreateError
            update.set("htmlCreateError", updateHtmlStatusBo.getHtmlCreateError());
        }

        mongoTemplate.updateFirst(query, update, ConversationEntity.class);
    }

    @Override
    public void updateCorrectStatus(UpdateCorrectStatusBo bo) {
        Query query = Query.query(Criteria.where("_id").is(bo.getId()));
        Update update = new Update();
        update.set("aiCorrectStatus", bo.getAiCorrectStatus());

        if (bo.getAiCorrectStatus() == AiEnums.correctStatus.CORRECTING.getCode()) {
            // 纠错中：设置来源类型
            if (ObjectUtil.isNotEmpty(bo.getAiCorrectType())) {
                update.set("aiCorrectType", String.valueOf(bo.getAiCorrectType()));
            }
            update.set("aiCorrectCreateTime", new Date().getTime());
        } else if (bo.getAiCorrectStatus() == AiEnums.correctStatus.CORRECTED.getCode()) {
            // 纠错完成：更新content
            if (ObjectUtil.isNotEmpty(bo.getContent())) {
                update.set("content", bo.getContent());
            }
        } else if (bo.getAiCorrectStatus() == AiEnums.correctStatus.FAIL.getCode()) {
            // 纠错失败：设置错误原因
            if (ObjectUtil.isNotEmpty(bo.getAiCorrectError())) {
                update.set("aiCorrectError", bo.getAiCorrectError());
            }
        }

        mongoTemplate.updateFirst(query, update, ConversationEntity.class);
    }
}
