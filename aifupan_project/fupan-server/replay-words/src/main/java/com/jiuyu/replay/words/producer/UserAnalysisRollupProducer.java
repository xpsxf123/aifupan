package com.jiuyu.replay.words.producer;

import com.jiuyu.replay.generic.utils.PageUtils;

import com.jiuyu.replay.generic.vo.power.UserVo;
import com.jiuyu.replay.words.entity.*;
import com.jiuyu.replay.words.vo.UserAnalysisRollupListVo;
import com.jiuyu.replay.words.vo.UserAnalysisRollupInfoVo;
import com.jiuyu.replay.words.bo.UserAnalysisRollupBo;
import com.jiuyu.replay.words.bo.UserAnalysisRollupListBo;

import java.text.ParseException;
import java.util.List;


/**
 * 用户分析汇总表
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-01-19 16:56:41
 */
public interface UserAnalysisRollupProducer {


    /**
     * 用户分析汇总表列表
     * @param userAnalysisRollupListBo 用户分析汇总表列表查询参数
     * @return
     */
    PageUtils<UserAnalysisRollupListVo> queryPage(UserAnalysisRollupListBo userAnalysisRollupListBo);

    /**
    * 用户分析汇总表信息
    * @param id 用户分析汇总表id
    * @return
    */
    UserAnalysisRollupInfoVo info(Long id);

    /**
     * 新增用户分析汇总表
     * @param userAnalysisRollupBo 用户分析汇总表对象
     * @return
     */
     UserAnalysisRollupInfoVo save(UserAnalysisRollupBo userAnalysisRollupBo);

    /**
     * 修改用户分析汇总表
     * @param userAnalysisRollupBo 用户分析汇总表对象
     * @return
     */
    void update(UserAnalysisRollupBo userAnalysisRollupBo);

    /**
     * 删除用户分析汇总表
     * @param id 用户分析汇总表id
     * @return
     */
    void deleteById(Long id);


    /**
     * 定时更新用户分析汇总数据
     *
     * @param listUserIds          所有用户ID
     * @param videoAnalysisRecords 所有视频分析记录
     * @param fileAnalysisRecords  所有文件分析记录
     * @param anchorVideoEntities
     * @param uploadFileEntities
     * @return
     */
    void timingUpdateData(List<Long> listUserIds, List<VideoAnalysisRecordEntity> videoAnalysisRecords, List<UploadFileAnalysisRecordEntity> fileAnalysisRecords, List<AnchorVideoEntity> anchorVideoEntities, List<UploadFileEntity> uploadFileEntities) throws ParseException;

    void timingUpdateDataAll(List<UserVo> userList);
    /**
     * 根据日平均分析查询
     * @param startDayAnalysis 日平均分析时间(开始查询条件)
     * @param endDayAnalysis 日平均分析时间(结束查询条件)
     * @return
     */
    List<Long> getUserIdsByDayAnalysis(Long startDayAnalysis, Long endDayAnalysis);

    /**
     * 根据多久未分析去查询用户
     * @param startLongNotAnalysis 多久未分析时间(开始查询条件)
     * @param endLongNotAnalysis 多久未分析时间(结束查询条件)
     * @return
     */
    List<Long> getUserIdsByNotAnalysis(Long startLongNotAnalysis, Long endLongNotAnalysis);

    /**
     * 查询所有用户的分析汇总
     * @return
     */
    List<UserAnalysisRollupEntity> listAll();
}

