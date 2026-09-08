package com.jiuyu.replay.words.rse.impl;

import com.jiuyu.replay.generic.dto.words.UserNotesCountDto;
import com.jiuyu.replay.words.entity.VideoTextNotes;
import com.jiuyu.replay.words.repository.mongo.VideoTextNotesRepository;
import com.jiuyu.replay.words.repository.mongo.VideoTextNotesRepositoryService;
import com.jiuyu.replay.words.rse.VideoTextNotesRse;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 视频文本笔记RSE实现类
 *
 * @author AI Assistant
 */
@Service
public class VideoTextNotesRseImpl implements VideoTextNotesRse {

    @Resource
    private VideoTextNotesRepositoryService videoTextNotesRepositoryService;
    @Resource
    private VideoTextNotesRepository videoTextNotesRepository;

    @Override
    public List<UserNotesCountDto> getYesterdayNotesCountByUserIds(List<Long> userIds, Long tenantId) {
        return videoTextNotesRepositoryService.getYesterdayNotesCountByUserIds(userIds, tenantId);
    }

    @Override
    public List<VideoTextNotes> listBySourceIdsAndNotesType(List<String> sourceIds, int notesType) {

        return videoTextNotesRepository.listBySourceIdsAndNotesType(sourceIds, 2);
    }
}
