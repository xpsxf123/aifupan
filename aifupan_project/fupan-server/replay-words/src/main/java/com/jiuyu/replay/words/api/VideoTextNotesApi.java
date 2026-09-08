package com.jiuyu.replay.words.api;

import com.jiuyu.replay.generic.dto.words.UserNotesCountDto;
import com.jiuyu.replay.generic.feign.words.VideoTextNotesFeign;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.words.bll.VideoTextNotesBll;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 视频文本笔记API实现
 *
 * @author AI Assistant
 */
@Service
public class VideoTextNotesApi implements VideoTextNotesFeign {

    @Resource
    private VideoTextNotesBll videoTextNotesBll;

    @Override
    public R<List<UserNotesCountDto>> getYesterdayNotesCountByUserIds(List<Long> userIds, Long tenantId) {
        return videoTextNotesBll.getYesterdayNotesCountByUserIds(userIds, tenantId);
    }
}
