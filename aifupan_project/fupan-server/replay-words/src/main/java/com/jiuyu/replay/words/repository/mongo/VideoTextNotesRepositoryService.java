package com.jiuyu.replay.words.repository.mongo;

import com.jiuyu.replay.generic.dto.words.UserNotesCountDto;

import java.util.List;

/**
 * 视频文本笔记Repository服务接口
 *
 * @author AI Assistant
 */
public interface VideoTextNotesRepositoryService {

    /**
     * 根据用户ID列表获取每个用户昨日的小结数量
     * 只统计sourceType = 0和notesType = 2的记录
     *
     * @param userIds 用户ID列表
     * @param tenantId 租户id
     * @return 用户昨日小结数量列表
     */
    List<UserNotesCountDto> getYesterdayNotesCountByUserIds(List<Long> userIds, Long tenantId);

    /**
     * 根据用户ID获取昨日的小结sourceId集合
     * @param userId 用户ID
     * @param tenantId 租户id
     * @return 昨日小结的sourceId集合
     */
    List<String> getYesterdayReviewNotesSourceIdsByUserId(Long userId, Long tenantId);
}
