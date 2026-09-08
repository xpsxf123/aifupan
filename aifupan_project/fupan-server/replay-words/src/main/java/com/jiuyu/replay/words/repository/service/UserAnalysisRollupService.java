package com.jiuyu.replay.words.repository.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.jiuyu.replay.words.entity.UserAnalysisRollupEntity;
import com.jiuyu.replay.words.vo.analysisRollup.AnalysisRollupVo;
import com.jiuyu.replay.generic.vo.agent.InviteUrlCodeAndPromotionName;

import java.util.List;

/**
 * 用户分析汇总表
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-01-19 16:56:41
 */
public interface UserAnalysisRollupService extends IService<UserAnalysisRollupEntity> {


    /**
     * 获取全部分组后的视频分析记录
     *
     * @return 数据
     */
    List<AnalysisRollupVo> videoAnalysisGroupAll();

    /**
     * 获取全部分组后的文件分析记录
     *
     * @return 数据
     */
    List<AnalysisRollupVo> fileAnalysisGroupAll();

    /**
     * 获取全部分组后的对比分析记录
     *
     * @return 数据
     */
    List<AnalysisRollupVo> contrastAnalysisGroupAll();

    /**
     * 获取全部分组后的自用抖音号数量
     *
     * @return 数据
     */
    List<AnalysisRollupVo> ownCountGroupAll();

    /**
     * 获取全部分组后的邀请链接code和渠道名称
     *
     * @return 数据
     */
    List<InviteUrlCodeAndPromotionName> inviteCodeAndPromotionNameGroupAll();
}

