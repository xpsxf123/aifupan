package com.jiuyu.replay.api.logic.third.impl;

import com.jiuyu.replay.api.logic.third.AudioDiscernLogic;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.power.utils.GlobalObject;
import com.jiuyu.replay.power.vo.UserCacheVo;
import com.jiuyu.replay.third.constant.Constant;
import com.jiuyu.replay.common.tencent.TencentAudioUtils;
import com.jiuyu.replay.common.vo.TencentTempTokenVo;
import com.jiuyu.replay.common.vo.CheckSurplusVo;
import com.tencentcloudapi.common.exception.TencentCloudSDKException;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

@Service
public class AudioDiscernLogicImpl implements AudioDiscernLogic {

    @Resource
    private TencentAudioUtils tencentAudioUtils;


    @Override
    public R<CheckSurplusVo> checkSurplus() {

        UserCacheVo user = GlobalObject.getLocalUser();

        CheckSurplusVo checkSurplusVo = tencentAudioUtils.checkSurplus(user.getId());

        if(checkSurplusVo != null) {

            return R.ok("还有余量", checkSurplusVo);
        }

        return R.error(Constant.CodeMsgEnum.NOT_QPS.getCode(), "Qps已满，请稍后再试");
    }

    @Override
    public R<String> addSurplus(Long id, String secretId) {

        tencentAudioUtils.addSurplus(id, secretId);

        return R.ok("操作成功");
    }

    @Override
    public R<TencentTempTokenVo> getTempToken(String secretId) throws TencentCloudSDKException {

        UserCacheVo user = GlobalObject.getLocalUser();
        TencentTempTokenVo tempTokenVo = tencentAudioUtils.getTempToken(user.getId(), secretId, "asr:SentenceRecognition");
        return R.ok("获取成功", tempTokenVo);
    }

    @Override
    public void record() {

//        UserCacheVo user = GlobalObject.getLocalUser();
//
//        tencentAudioUtils.record(user.getId());

    }

    @Override
    public R<TencentTempTokenVo> getRecTempToken() throws TencentCloudSDKException {

        UserCacheVo user = GlobalObject.getLocalUser();
        TencentTempTokenVo tempTokenVo = tencentAudioUtils.getTempToken(user.getId(), "", "asr:CreateRecTask");
        return R.ok("获取成功", tempTokenVo);
    }


}
