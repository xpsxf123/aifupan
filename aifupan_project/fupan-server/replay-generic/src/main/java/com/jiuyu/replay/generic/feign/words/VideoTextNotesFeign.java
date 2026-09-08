package com.jiuyu.replay.generic.feign.words;

import com.jiuyu.replay.generic.dto.words.UserNotesCountDto;
import com.jiuyu.replay.generic.vo.common.R;

import java.util.List;

/**
 * 视频文本笔记Feign接口
 *
 * @author AI Assistant
 */
public interface VideoTextNotesFeign {

    /**
     * 根据用户ID列表获取每个用户昨日的小结数量
     * 只统计sourceType = 0和notesType = 2的记录
     *
     * @param userIds 用户ID列表
     * @param tenantId 租户id
     * @return 用户昨日小结数量列表
     */
    R<List<UserNotesCountDto>> getYesterdayNotesCountByUserIds(List<Long> userIds, Long tenantId);
}
