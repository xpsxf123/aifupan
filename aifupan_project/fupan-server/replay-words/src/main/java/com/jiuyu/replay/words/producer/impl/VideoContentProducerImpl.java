package com.jiuyu.replay.words.producer.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.jiuyu.replay.common.utils.RRException;
import com.jiuyu.replay.generic.vo.words.VideoContentVo;
import com.jiuyu.replay.words.entity.VideoContentEntity;
import com.jiuyu.replay.words.producer.VideoContentProducer;
import com.jiuyu.replay.words.repository.mongo.VideoContentRepository;
import com.mongodb.client.result.UpdateResult;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * @author ：lujie
 * @description：
 * @date ：2025/6/25 上午10:30
 */
@Service
@AllArgsConstructor
public class VideoContentProducerImpl implements VideoContentProducer {

    private final VideoContentRepository videoContentRepository;
    private final MongoTemplate mongoTemplate;

    @Override
    public List<VideoContentVo> listBySourceIdAndStatus(String sourceId, Integer type, Long userId, Long tenantId, int generateStatus) {

        VideoContentEntity exampleEntity = new VideoContentEntity();
        exampleEntity.setSourceId(sourceId);
        exampleEntity.setType(type);
        exampleEntity.setUserId(userId);
        exampleEntity.setTenantId(tenantId);
        exampleEntity.setGenerateStatus(generateStatus);
        exampleEntity.setIsDeleted(0);

        // 创建匹配器，忽略 null 值和空字符串
        ExampleMatcher matcher = ExampleMatcher.matching()
                .withIgnoreNullValues()
                .withStringMatcher(ExampleMatcher.StringMatcher.EXACT);

        Example<VideoContentEntity> example = Example.of(exampleEntity, matcher);
        // 添加排序
        Sort sort = Sort.by(Sort.Direction.ASC, "paragraph");
        List<VideoContentEntity> list = videoContentRepository.findAll(example, sort);
        if (ObjectUtil.isNotEmpty(list)) {
            return BeanUtil.copyToList(list, VideoContentVo.class);
        }
        return new ArrayList<>();
    }

    /**
     * 批量保存分段数据到 MongoDB。
     *
     * <h3>时间字段处理</h3>
     * 如果调用方未设置 createDate（新创建的分段），自动设置为当前时间。
     * updateDate 始终更新为当前时间。
     *
     * @param voList 分段 VO 列表
     * @return 保存后的分段列表（含 MongoDB 自动生成的 _id）
     */
    @Override
    public List<VideoContentVo> saveAll(List<VideoContentVo> voList) {
        if (voList == null || voList.isEmpty()) {
            return new ArrayList<>();
        }
        Date now = new Date();
        List<VideoContentEntity> videoContentEntities = BeanUtil.copyToList(voList, VideoContentEntity.class);
        videoContentEntities.forEach(e -> {
            // 保留调用方设置的 createDate（理论上不会设置），未设置时自动填充
            if (e.getCreateDate() == null) {
                e.setCreateDate(now);
            }
            e.setIsDeleted(0);
            e.setUpdateDate(now);
        });
        List<VideoContentEntity> videoContentEntities1 = videoContentRepository.saveAll(videoContentEntities);
        if (ObjectUtil.isNotEmpty(videoContentEntities1)) {
            return BeanUtil.copyToList(videoContentEntities1, VideoContentVo.class);
        }
        return new ArrayList<>();
    }

    /**
     * 部分更新分段：content + generateStatus + updateDate。
     * 用于分段 AI 生成完成后回写结果。
     *
     * <h3>注意</h3>
     * 此方法只更新三个字段（content、generateStatus、updateDate），
     * 不会修改其他已有字段（如 cueWord、paragraph、sourceId 等）。
     *
     * @param contentVo 需含 id（MongoDB _id）、content（生成的内容）
     * @throws RRException 匹配不到对应文档时抛出
     */
    @Override
    public void updateById(VideoContentVo contentVo) {
        Update update = new Update();
        update.set("content", contentVo.getContent());
        update.set("generateStatus", 1);
        update.set("updateDate", new Date());
        UpdateResult res = mongoTemplate.updateFirst(Query.query(Criteria.where("_id").is(contentVo.getId())), update, VideoContentEntity.class);
        if (res.getMatchedCount() <= 0) {
            RRException.create("更新失败");
        }
    }

    @Override
    public String formatContent(String content) {
        if (StrUtil.isBlank(content)) {
            return content;
        }
        List<String> lines = Arrays.stream(content.split("\\r?\\n"))
                .filter(StrUtil::isNotBlank)
                .map(String::trim)
                .filter(line -> !line.matches("^[\\p{P}\\p{S}]+$"))
                .toList();
        return JSONUtil.toJsonStr(lines);
    }

    @Override
    public boolean checkContentAllComplete(String sourceId, Integer type, Long userId, Long tenantId) {
        // 查询还存在未完成的记录
        VideoContentEntity exampleEntity = new VideoContentEntity();
        exampleEntity.setSourceId(sourceId);
        exampleEntity.setType(type);
        exampleEntity.setUserId(userId);
        exampleEntity.setTenantId(tenantId);
        exampleEntity.setGenerateStatus(0);
        exampleEntity.setIsDeleted(0);

        // 创建匹配器，忽略 null 值和空字符串
        ExampleMatcher matcher = ExampleMatcher.matching()
                .withIgnoreNullValues()
                .withStringMatcher(ExampleMatcher.StringMatcher.EXACT);

        Example<VideoContentEntity> example = Example.of(exampleEntity, matcher);

        long count = videoContentRepository.count(example);
        return count == 0;
    }
}
