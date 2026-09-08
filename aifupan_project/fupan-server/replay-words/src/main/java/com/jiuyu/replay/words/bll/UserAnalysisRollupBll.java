package com.jiuyu.replay.words.bll;

import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;

import com.jiuyu.replay.generic.vo.power.UserVo;
import com.jiuyu.replay.words.entity.*;
import com.jiuyu.replay.words.vo.UserAnalysisRollupListVo;
import com.jiuyu.replay.words.vo.UserAnalysisRollupInfoVo;
import com.jiuyu.replay.words.bo.UserAnalysisRollupBo;
import com.jiuyu.replay.words.bo.UserAnalysisRollupListBo;
import com.jiuyu.replay.words.producer.UserAnalysisRollupProducer;

import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.text.ParseException;
import java.util.List;


/**
 * 用户分析汇总表
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-01-19 16:56:41
 */
@Component
public class UserAnalysisRollupBll {

    @Resource
    private UserAnalysisRollupProducer userAnalysisRollupProducer;


    /**
     * 用户分析汇总表列表
     * @param userAnalysisRollupListBo 用户分析汇总表列表查询参数
     * @return
     */
    public R<PageUtils<UserAnalysisRollupListVo>> queryPage(UserAnalysisRollupListBo userAnalysisRollupListBo) {

        return R.ok("获取成功", userAnalysisRollupProducer.queryPage(userAnalysisRollupListBo));
    }

    /**
    * 用户分析汇总表信息
    * @param id 用户分析汇总表id
    * @return
    */
    public R<UserAnalysisRollupInfoVo> info(Long id) {

        UserAnalysisRollupInfoVo userAnalysisRollupInfoVo = userAnalysisRollupProducer.info(id);
        return R.ok("获取成功", userAnalysisRollupInfoVo);
    }

    /**
     * 新增用户分析汇总表
     * @param userAnalysisRollupBo 用户分析汇总表对象
     * @return
     */
    public R<String> save(UserAnalysisRollupBo userAnalysisRollupBo) {

        UserAnalysisRollupInfoVo userAnalysisRollupInfoVo = userAnalysisRollupProducer.save(userAnalysisRollupBo);
        return R.ok("添加成功");
    }

    /**
     * 修改用户分析汇总表
     * @param userAnalysisRollupBo 用户分析汇总表对象
     * @return
     */
    public R<String> update(UserAnalysisRollupBo userAnalysisRollupBo) {

        userAnalysisRollupProducer.update(userAnalysisRollupBo);
        return R.ok("修改成功");
    }

    /**
     * 删除用户分析汇总表
     * @param id 用户分析汇总表id
     * @return
     */
    public R<String> delete(Long id) {

        userAnalysisRollupProducer.deleteById(id);
        return R.ok("删除成功");
    }


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
    public void timingUpdateData(List<Long> listUserIds, List<VideoAnalysisRecordEntity> videoAnalysisRecords, List<UploadFileAnalysisRecordEntity> fileAnalysisRecords, List<AnchorVideoEntity> anchorVideoEntities, List<UploadFileEntity> uploadFileEntities) throws ParseException {
        userAnalysisRollupProducer.timingUpdateData(listUserIds,videoAnalysisRecords,fileAnalysisRecords,anchorVideoEntities,uploadFileEntities);
    }

    /**
     * 根据日平均分析查询
     * @param startDayAnalysis 日平均分析时间(开始查询条件)
     * @param endDayAnalysis 日平均分析时间(结束查询条件)
     * @return
     */
    public List<Long> getUserIdsByDayAnalysis(Long startDayAnalysis, Long endDayAnalysis) {
        return userAnalysisRollupProducer.getUserIdsByDayAnalysis(startDayAnalysis,endDayAnalysis);
    }

    /**
     * 根据多久未分析去查询用户
     * @param startLongNotAnalysis 多久未分析时间(开始查询条件)
     * @param endLongNotAnalysis 多久未分析时间(结束查询条件)
     * @return
     */
    public List<Long> getUserIdsByNotAnalysis(Long startLongNotAnalysis, Long endLongNotAnalysis) {
        return userAnalysisRollupProducer.getUserIdsByNotAnalysis(startLongNotAnalysis,endLongNotAnalysis);
    }

    /**
     * 查询所有用户的分析汇总
     * @return
     */
    public List<UserAnalysisRollupEntity> listAll() {
        return userAnalysisRollupProducer.listAll();
    }

    public void timingUpdateDataAll(List<UserVo> userList) {
        userAnalysisRollupProducer.timingUpdateDataAll(userList);
    }
}

