package com.jiuyu.replay.api.logic.third;

import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.common.vo.TencentTempTokenVo;
import com.jiuyu.replay.common.vo.CheckSurplusVo;
import com.tencentcloudapi.common.exception.TencentCloudSDKException;

public interface AudioDiscernLogic {

    /**
     * 查询是否还有调用语音识别接口的余量
     * @return
     */
    R<CheckSurplusVo> checkSurplus();

    /**
     * 通知加回语音识别接口的余量
     * @param id 补回余量的id
     * @param secretId 补回余量的secretId
     * @return
     */
    R<String> addSurplus(Long id, String secretId);

    /**
     * 获取语音识别接口临时调用凭证
     * @param secretId 语音识别secretId
     * @return
     */
    R<TencentTempTokenVo> getTempToken(String secretId) throws TencentCloudSDKException;

    /**
     * 记录QPS访问
     * @return
     */
    void record();

    /**
     * 获取长音频识别接口临时调用凭证
     * @return
     */
    R<TencentTempTokenVo> getRecTempToken() throws TencentCloudSDKException;
}
