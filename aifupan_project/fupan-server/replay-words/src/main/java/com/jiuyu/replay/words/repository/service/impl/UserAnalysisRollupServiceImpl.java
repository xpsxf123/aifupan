package com.jiuyu.replay.words.repository.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jiuyu.replay.words.entity.UserAnalysisRollupEntity;
import com.jiuyu.replay.words.repository.dao.UserAnalysisRollupDao;
import com.jiuyu.replay.words.repository.service.UserAnalysisRollupService;
import com.jiuyu.replay.words.vo.analysisRollup.AnalysisRollupVo;
import com.jiuyu.replay.generic.vo.agent.InviteUrlCodeAndPromotionName;
import org.springframework.stereotype.Service;

import java.util.List;


@Service("userAnalysisRollupService")
public class UserAnalysisRollupServiceImpl extends ServiceImpl<UserAnalysisRollupDao, UserAnalysisRollupEntity> implements UserAnalysisRollupService {

    @Override
    public List<AnalysisRollupVo> videoAnalysisGroupAll() {
        return baseMapper.videoAnalysisGroupAll();
    }

    @Override
    public List<AnalysisRollupVo> fileAnalysisGroupAll() {
        return baseMapper.fileAnalysisGroupAll();
    }

    @Override
    public List<AnalysisRollupVo> contrastAnalysisGroupAll() {
        return baseMapper.contrastAnalysisGroupAll();
    }

    @Override
    public List<AnalysisRollupVo> ownCountGroupAll() {
        return baseMapper.ownCountGroupAll();
    }

    @Override
    public List<InviteUrlCodeAndPromotionName> inviteCodeAndPromotionNameGroupAll() {
        return baseMapper.inviteCodeAndPromotionNameGroupAll();
    }
}