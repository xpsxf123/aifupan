package com.jiuyu.replay.generic.feign.words;

import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.generic.vo.words.AnalysisResultVo;
import com.jiuyu.replay.generic.vo.words.UploadFileSimpleInfoVo;

/**
 * @author ：lujie
 * @description：
 * @date ：2025/5/30 上午11:31
 */
public interface SensitiveWordsFeign {

    R<AnalysisResultVo> getAnalysisData(Integer type, String uuid);

    /**
     * 获取行业id
     *
     * @param sourceType 类型
     * @param sourceId   源id
     * @return 行业id
     */
    Long getTradeId(Integer sourceType, String sourceId);

    /**
     * 按 fileId 获取上传文件精简信息（跨模块资源校验用）。
     *
     * <p>fileId 不存在时返回 R.ok(null)。</p>
     *
     * @param fileId 文件唯一标识（uuid）
     * @return 上传文件精简信息，不存在时 data 为 null
     */
    R<UploadFileSimpleInfoVo> getUploadFileInfo(String fileId);
}
