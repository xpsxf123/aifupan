package com.jiuyu.replay.ai.bll;

import cn.hutool.core.date.DateUtil;
import com.jiuyu.replay.ai.bo.DiagnosisSignUploadUrlBo;
import com.jiuyu.replay.ai.bo.SaveDiagnosisBo;
import com.jiuyu.replay.ai.rse.DiagnosisCueRse;
import com.jiuyu.replay.common.alibaba.AiOssUtils;
import com.jiuyu.replay.common.utils.RRException;
import com.jiuyu.replay.common.vo.SignUploadUrlVo;
import com.jiuyu.replay.generic.bo.ai.DiagnosisCueBo;
import com.jiuyu.replay.generic.feign.power.UserFeign;
import com.jiuyu.replay.generic.feign.third.AiModelFeign;
import com.jiuyu.replay.generic.feign.third.PropertiesFeign;
import com.jiuyu.replay.generic.feign.words.AnchorVideoDetailFeign;
import com.jiuyu.replay.generic.feign.words.AnchorVideoFeign;
import com.jiuyu.replay.generic.utils.ResultUtil;
import com.jiuyu.replay.generic.vo.ai.DiagnosisCueInfoVo;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.generic.vo.words.AnchorVideoDetailInfoVo;
import com.jiuyu.replay.generic.vo.words.AnchorVideoInfoVo;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;

/**
 * @author ：lujie
 * @description：
 * @date ：2025/5/28 下午11:59
 */
@Component
@AllArgsConstructor
public class DiagnosisBll {

    private final AiOssUtils aiOssUtils;
    private final UserFeign userFeign;
    private final AnchorVideoFeign anchorVideoFeign;
    private final AnchorVideoDetailFeign anchorVideoDetailFeign;
    private final DiagnosisCueRse diagnosisCueRse;
    private final DiagnosisModelBll diagnosisModelBll;
    private final AiModelFeign aiModelFeign;
    private final PropertiesFeign propertiesFeign;

    public R<SignUploadUrlVo> getDiagnosisSignUploadUrl(DiagnosisSignUploadUrlBo urlBo) {
        return R.ok("获取成功", aiOssUtils.getSignUploadUrl(getDiagnosisOssKey(urlBo.getSourceId(), null)));
    }

    public R<String> getDiagnosisDownloadUrl(String videoId) {
        AnchorVideoInfoVo video = ResultUtil.getResult(anchorVideoFeign.GetByVideoId(videoId));
        RRException.isNotEmpty(video, "视频信息获取失败");
        RRException.isNotEmpty(video.getCreateDate(), "视频创建时间获取失败");

        AnchorVideoDetailInfoVo result = anchorVideoDetailFeign.getAndSave(video.getVideoId());
        RRException.isNotEmpty(result, "视频详情获取失败");
        if (result.getHasDiagnosisReport() == null || result.getHasDiagnosisReport() == 0){
            RRException.create("诊断报告获取失败");
        }

        String signDownloadUrl = aiOssUtils.getSignDownloadUrl(getDiagnosisOssKey(videoId, video.getCreateDate()));
        RRException.isNotEmpty(signDownloadUrl, "诊断报告获取失败");
        return R.ok(signDownloadUrl);
    }

    /**
     * 获取诊断报告的ossKey
     * @param videoId
     * @return
     */
    public String getDiagnosisOssKey(String videoId, Date createDate){
        if (createDate == null){
            AnchorVideoInfoVo video = ResultUtil.getResult(anchorVideoFeign.GetByVideoId(videoId));
            RRException.isNotEmpty(video, "视频信息获取失败");
            RRException.isNotEmpty(video.getCreateDate(), "视频创建时间获取失败");
            createDate = video.getCreateDate();
        }
        return AiOssUtils.diagnosisPrefix + DateUtil.format(createDate, "/yyyy/MM/dd/") + videoId + ".pdf";
    }

    /**
     * 诊断分析-提示词添加分析
     * @param saveDiagnosisBo
     */
    public List<DiagnosisCueInfoVo> saveDiagnosis(SaveDiagnosisBo saveDiagnosisBo) {
        return diagnosisCueRse.saveDiagnosis(saveDiagnosisBo);
    }

    /**
     * 获取用户待分析和分析中的诊断报告
     * @return
     */
    public List<DiagnosisCueInfoVo> handleDiagnosisByUser() {
        return diagnosisCueRse.handleDiagnosisByUser();
    }

    public R<String> updateDiagnosisCueStatus(DiagnosisCueBo diagnosisCueBo) {
        diagnosisCueRse.update(diagnosisCueBo);
        return R.ok("更新成功");
    }
}
