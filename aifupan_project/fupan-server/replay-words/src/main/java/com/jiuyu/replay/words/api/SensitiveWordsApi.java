package com.jiuyu.replay.words.api;

import com.jiuyu.replay.generic.feign.words.SensitiveWordsFeign;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.generic.vo.words.AnalysisResultVo;
import com.jiuyu.replay.generic.vo.words.UploadFileSimpleInfoVo;
import com.jiuyu.replay.words.bll.SensitiveWordsBll;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * @author ：lujie
 * @description：
 * @date ：2025/5/30 上午11:33
 */
@Component
@AllArgsConstructor
public class SensitiveWordsApi implements SensitiveWordsFeign {

    private final SensitiveWordsBll sensitiveWordsBll;


    @Override
    public R<AnalysisResultVo> getAnalysisData(Integer type, String uuid) {
        return sensitiveWordsBll.getAnalysisData(type, uuid);
    }

    @Override
    public Long getTradeId(Integer sourceType, String sourceId) {
        return sensitiveWordsBll.getTradeId(sourceType, sourceId);
    }

    @Override
    public R<UploadFileSimpleInfoVo> getUploadFileInfo(String fileId) {
        return sensitiveWordsBll.getUploadFileInfo(fileId);
    }
}
