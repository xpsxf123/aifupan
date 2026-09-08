package com.jiuyu.replay.words.repository.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jiuyu.replay.words.entity.UserAnalysisRollupEntity;
import com.jiuyu.replay.words.vo.analysisRollup.AnalysisRollupVo;
import com.jiuyu.replay.generic.vo.agent.InviteUrlCodeAndPromotionName;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 用户分析汇总表
 * 
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-01-19 16:56:41
 */
@Mapper
public interface UserAnalysisRollupDao extends BaseMapper<UserAnalysisRollupEntity> {


    /**
     * 获取全部分组后的视频分析记录
     *
     * @return 数据
     */
    @Select("SELECT user_id, MIN(create_date) AS min_time, MAX(create_date) AS max_time, COUNT(*) AS count FROM tb_video_analysis_record GROUP BY user_id")
    List<AnalysisRollupVo> videoAnalysisGroupAll();

    /**
     * 获取全部分组后的文件分析记录
     *
     * @return 数据
     */
    @Select("SELECT user_id, MIN(create_date) AS min_time, MAX(create_date) AS max_time, COUNT(*) AS count FROM tb_upload_file_analysis_record GROUP BY user_id")
    List<AnalysisRollupVo> fileAnalysisGroupAll();

    /**
     * 获取全部分组后的对比分析记录
     *
     * @return 数据
     */
    @Select("SELECT user_id, COUNT(*) AS count FROM tb_sync_contrast where is_deleted = 0 GROUP BY user_id")
    List<AnalysisRollupVo> contrastAnalysisGroupAll();

    /**
     * 获取全部分组后的自用抖音号数量
     *
     * @return 数据
     */
    @Select("select user_id, tenant_id, count(*) as count from tb_anchor_url_user where is_deleted = 0 and is_remove_record = 0 and account_type = 0 GROUP BY user_id, tenant_id")
    List<AnalysisRollupVo> ownCountGroupAll();

    /**
     * 获取全部分组后的邀请链接code和渠道名称
     *
     * @return 数据
     */
    @Select("select iuc.url_code, ap.promotion_name from tb_invite_url_code iuc left join tb_agent_promotion ap on iuc.promotion_id = ap.id where iuc.is_deleted = 0 and iuc.promotion_id > 0 GROUP BY iuc.url_code")
    List<InviteUrlCodeAndPromotionName> inviteCodeAndPromotionNameGroupAll();
}
