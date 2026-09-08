package com.jiuyu.replay.words.repository.mongo.impl;

import com.jiuyu.replay.generic.dto.words.UserNotesCountDto;
import com.jiuyu.replay.words.entity.VideoTextNotes;
import com.jiuyu.replay.words.repository.mongo.VideoTextNotesRepositoryService;
import jakarta.annotation.Resource;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 视频文本笔记Repository服务实现类
 *
 * @author AI Assistant
 */
@Service
public class VideoTextNotesRepositoryServiceImpl implements VideoTextNotesRepositoryService {

        @Resource
        private MongoTemplate mongoTemplate;

        @Override
        public List<UserNotesCountDto> getYesterdayNotesCountByUserIds(List<Long> userIds, Long tenantId) {
                if (userIds == null || userIds.isEmpty()) {
                        return new ArrayList<>();
                }

                // 计算昨日的开始和结束时间
                LocalDate yesterday = LocalDate.now().minusDays(1);
                LocalDateTime startOfYesterday = yesterday.atStartOfDay();
                LocalDateTime endOfYesterday = yesterday.atTime(LocalTime.MAX);

                // 使用MongoDB聚合查询统计每个用户昨日的小结数量
                Aggregation aggregation = Aggregation.newAggregation(
                                // 匹配条件：sourceType = 0, notesType = 2, 创建用户在指定列表中, 创建时间在昨日, 未删除
                                Aggregation.match(Criteria.where("sourceType").is(0)
                                                .and("notesType").is(2)
                                                .and("tenantId").is(tenantId)
                                                .and("userId").in(userIds)
                                                .and("createTime").gte(startOfYesterday).lte(endOfYesterday)
                                                .and("isDeleted").is(false)),
                                // 按创建用户ID分组并计数
                                Aggregation.group("userId").count().as("notesCount"),
                                // 重命名字段(将createUserId重命名为userId，group之后createUserId会变成_id)
                                Aggregation.project("notesCount").and("_id").as("userId"));

                AggregationResults<UserNotesCountDto> results = mongoTemplate.aggregate(
                                aggregation, VideoTextNotes.class, UserNotesCountDto.class);

                List<UserNotesCountDto> resultList = results.getMappedResults();

                // 确保所有用户都有记录，没有记录的用户设置为0
                Map<Long, Integer> countMap = resultList.stream()
                                .collect(Collectors.toMap(UserNotesCountDto::getUserId,
                                                UserNotesCountDto::getNotesCount));

                return userIds.stream().map(userId -> {
                        UserNotesCountDto dto = new UserNotesCountDto();
                        dto.setUserId(userId);
                        dto.setNotesCount(countMap.getOrDefault(userId, 0));
                        return dto;
                }).collect(Collectors.toList());
        }

        @Override
        public List<String> getYesterdayReviewNotesSourceIdsByUserId(Long userId, Long tenantId) {
                if (userId == null) {
                        return new ArrayList<>();
                }

                // 计算昨日的开始和结束时间
                LocalDate yesterday = LocalDate.now().minusDays(1);
                LocalDateTime startOfYesterday = yesterday.atStartOfDay();
                LocalDateTime endOfYesterday = yesterday.atTime(LocalTime.MAX);

                // 使用MongoDB聚合查询获取昨日该用户的小结sourceId集合
                Aggregation aggregation = Aggregation.newAggregation(
                                // 匹配条件：sourceType = 0, notesType = 2, 创建用户为指定用户, 创建时间在昨日, 未删除
                                Aggregation.match(Criteria.where("sourceType").is(0)
                                                .and("notesType").is(2)
                                                .and("userId").is(userId)
                                                .and("createTime").gte(startOfYesterday).lte(endOfYesterday)
                                                .and("isDeleted").is(false)),
                                // 只选择sourceId字段
                                Aggregation.project("sourceId"));

                AggregationResults<VideoTextNotes> results = mongoTemplate.aggregate(
                                aggregation, VideoTextNotes.class, VideoTextNotes.class);

                List<VideoTextNotes> mappedResults = results.getMappedResults();
                if (mappedResults.isEmpty()) {
                        return new ArrayList<>();
                }

                return mappedResults.stream().map(VideoTextNotes::getSourceId).collect(Collectors.toList());
        }
}
