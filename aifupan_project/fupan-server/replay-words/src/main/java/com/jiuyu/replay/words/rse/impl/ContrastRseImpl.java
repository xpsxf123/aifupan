package com.jiuyu.replay.words.rse.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.jiuyu.replay.common.utils.BeanConvertUtils;
import com.jiuyu.replay.generic.dto.words.ShareContrastCloudDto;
import com.jiuyu.replay.words.constant.WordsProperties;
import com.jiuyu.replay.words.entity.SyncContrastEntity;
import com.jiuyu.replay.words.repository.service.SyncContrastService;
import com.jiuyu.replay.words.rse.ContrastRse;
import com.jiuyu.replay.words.vo.SyncContrastInfoVo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class ContrastRseImpl implements ContrastRse {

    @Resource
    private SyncContrastService syncContrastService;
    @Resource
    private WordsProperties wordsProperties;

    @Override
    public SyncContrastInfoVo infoByContrastIdAndUserId(String contrastId, Long userId) {

        QueryWrapper<SyncContrastEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("contrast_id", contrastId);
        wrapper.eq("user_id", userId);
        SyncContrastEntity syncContrastEntity = this.syncContrastService.getOne(wrapper);
        if(syncContrastEntity != null) {
            return BeanConvertUtils.convert(syncContrastEntity, SyncContrastInfoVo.class);
        }
        return null;
    }

    @Override
    public String shareContrastToCloud(ShareContrastCloudDto shareContrastCloudDto) {

        SyncContrastEntity syncContrastEntity = BeanConvertUtils.convert(shareContrastCloudDto, SyncContrastEntity.class);
        syncContrastEntity.setIsShard(1);
        syncContrastEntity.setUpdateDate(new Date());
        String shareUrl = wordsProperties.getCloudSpaceUrl() + "contrastOnlineAnalysis/" + shareContrastCloudDto.getContrastId();
        syncContrastEntity.setShareUrl(shareUrl);
        this.syncContrastService.updateById(syncContrastEntity);

        return shareUrl;
    }

}
