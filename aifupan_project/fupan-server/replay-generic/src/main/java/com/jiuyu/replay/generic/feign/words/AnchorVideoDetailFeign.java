package com.jiuyu.replay.generic.feign.words;

import com.jiuyu.replay.generic.vo.words.AnchorVideoDetailInfoVo;

public interface AnchorVideoDetailFeign {

    /**
     * 视频的详情信息
     *
     * @param videoId
     * @return
     */
    AnchorVideoDetailInfoVo getAndSave(String videoId);

    /**
     * 更新视频是否上传诊断报告字段
     *
     * @param videoId                视频唯一标识
     * @param hasDiagnosisReport     是否有诊断报告
     * @param diagnosisOssName       最新诊断报告文件名称
     * @param hasDataDiagnosisReport 是否有数据诊断报告
     * @param dataDiagnosisOssName   最新数据诊断报告文件名称
     */
    void updateHasDiagnosisReport(String videoId, Integer hasDiagnosisReport, String diagnosisOssName, Integer hasDataDiagnosisReport, String dataDiagnosisOssName);

}
