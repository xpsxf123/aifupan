package com.jiuyu.replay.words.bll;

import com.jiuyu.replay.generic.dto.words.UserNotesCountDto;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.words.rse.VideoTextNotesRse;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 视频文本笔记业务逻辑层
 *
 * @author AI Assistant
 */
@Component
public class VideoTextNotesBll {

    @Resource
    private VideoTextNotesRse videoTextNotesRse;

    /**
     * 根据用户ID列表获取每个用户昨日的小结数量
     * 只统计sourceType = 0和notesType = 2的记录
     *
     * @param userIds 用户ID列表
     * @param tenantId 租户id
     * @return 用户昨日小结数量列表
     */
    public R<List<UserNotesCountDto>> getYesterdayNotesCountByUserIds(List<Long> userIds, Long tenantId) {
        List<UserNotesCountDto> result = videoTextNotesRse.getYesterdayNotesCountByUserIds(userIds, tenantId);
        return R.ok("获取成功", result);
    }
}
