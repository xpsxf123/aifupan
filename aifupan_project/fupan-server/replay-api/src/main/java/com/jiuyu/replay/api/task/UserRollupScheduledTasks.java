package com.jiuyu.replay.api.task;


import cn.hutool.core.bean.BeanUtil;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.power.bll.UserBll;
import com.jiuyu.replay.power.vo.UserVo;
import com.jiuyu.replay.words.bll.*;
import com.xxl.job.core.handler.annotation.XxlJob;
import jakarta.annotation.Resource;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.text.ParseException;
import java.util.List;

@Component
@AllArgsConstructor
@Slf4j
public class UserRollupScheduledTasks {

    @Resource
    private UserAnalysisRollupBll userAnalysisRollupBll;
    @Resource
    private UserBll userBll;
    @Resource
    private VideoAnalysisRecordBll videoAnalysisRecordBll;
    @Resource
    private UploadFileAnalysisRecordBll uploadFileAnalysisRecordBll;
    @Resource
    private AnchorVideoBll anchorVideoBll;
    @Resource
    private UploadFileBll uploadFileBll;

    /**
     * 定时更新用户分析汇总数据
     * @return
     * @throws ParseException
     */
    // 使用调度器配置执行cron时间 @Scheduled(cron = "0 0 2 * * ?")
    @XxlJob("timingUpdateData")
    public R<String> timingUpdateData() throws ParseException {
//        List<Long> listUserIds =  userBll.listUserId();// 查询所有用户ID
//        List<VideoAnalysisRecordEntity> videoAnalysisRecords =  videoAnalysisRecordBll.UserIdAndRecord(); // 查询所有视频分析记录
//        List<UploadFileAnalysisRecordEntity> FileAnalysisRecords = uploadFileAnalysisRecordBll.UserIdAndRecord(); // 查询所有文件分析记录
//        List<AnchorVideoEntity> anchorVideoEntities=  anchorVideoBll.listAllByStatus(); // 查询anchorVideo表得到最早的分析时间
//        List<UploadFileEntity> uploadFileEntities =  uploadFileBll.listAllByStatus();
//
//        userAnalysisRollupBll.timingUpdateData(listUserIds,videoAnalysisRecords,FileAnalysisRecords,anchorVideoEntities,uploadFileEntities);
        log.info("[定时更新用户分析汇总数据] 开始执行");
        long start = System.currentTimeMillis();
        List<UserVo> userList = userBll.listUserRollupFieldAll();
        userAnalysisRollupBll.timingUpdateDataAll(BeanUtil.copyToList(userList, com.jiuyu.replay.generic.vo.power.UserVo.class));
        log.info("[定时更新用户分析汇总数据] 执行完成 用时{}ms", System.currentTimeMillis() - start);
        return R.ok("");
    }
}
