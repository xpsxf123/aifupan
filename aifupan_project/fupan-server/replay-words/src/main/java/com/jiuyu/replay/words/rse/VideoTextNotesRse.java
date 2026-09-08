package com.jiuyu.replay.words.rse;

import com.jiuyu.replay.generic.dto.words.UserNotesCountDto;
import com.jiuyu.replay.words.entity.VideoTextNotes;

import java.util.List;

/**
 * 视频文本笔记RSE接口
 *
 * @author AI Assistant
 */
public interface VideoTextNotesRse {

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
     * 根据来源id集合和笔记类型获取小结列表
     *
     * @param sourceIds 来源id集合
     * @param notesType 笔记类型
     * @return 返回匹配的VideoTextNotes列表
     */
    List<VideoTextNotes> listBySourceIdsAndNotesType(List<String> sourceIds, int notesType);
}
