package com.jiuyu.replay.words.api;

import com.jiuyu.replay.generic.feign.words.AnchorVideoDetailFeign;
import com.jiuyu.replay.generic.vo.words.AnchorVideoDetailInfoVo;
import com.jiuyu.replay.words.bll.AnchorVideoDetailBll;
import com.jiuyu.replay.words.bo.video.AnchorVideoDetailBo;
import com.jiuyu.replay.words.producer.AnchorVideoDetailProducer;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * @author ：lujie
 * @description：
 * @date ：2025/5/29 下午5:42
 */
@Component
@AllArgsConstructor
public class AnchorVideoDetailApi implements AnchorVideoDetailFeign {

    private final AnchorVideoDetailBll anchorVideoDetailBll;
    private final AnchorVideoDetailProducer anchorVideoDetailProducer;


    @Override
    public AnchorVideoDetailInfoVo getAndSave(String videoId) {
        AnchorVideoDetailBo bo = new AnchorVideoDetailBo();
        bo.setVideoId(videoId);
        return anchorVideoDetailBll.getAndSave(bo).getData();
    }

    @Override
    public void updateHasDiagnosisReport(String videoId, Integer hasDiagnosisReport, String diagnosisOssName, Integer hasDataDiagnosisReport, String dataDiagnosisOssName) {
        AnchorVideoDetailBo bo = new AnchorVideoDetailBo();
        bo.setVideoId(videoId);
        AnchorVideoDetailInfoVo result = anchorVideoDetailBll.getAndSave(bo).getData();

        if (result != null && result.getId() != null) {
            AnchorVideoDetailBo detailBo = new AnchorVideoDetailBo();
            detailBo.setId(result.getId());
            detailBo.setHasDiagnosisReport(hasDiagnosisReport);
            detailBo.setDiagnosisOssName(diagnosisOssName);
            detailBo.setHasDataDiagnosisReport(hasDataDiagnosisReport);
            detailBo.setDataDiagnosisOssName(dataDiagnosisOssName);
            anchorVideoDetailProducer.update(detailBo);
        }
    }
}
