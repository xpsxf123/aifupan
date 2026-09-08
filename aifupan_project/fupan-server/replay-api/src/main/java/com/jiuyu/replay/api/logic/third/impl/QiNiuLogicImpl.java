package com.jiuyu.replay.api.logic.third.impl;

import com.jiuyu.replay.api.logic.third.QiNiuLogic;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.third.qiniu.QiNiuOssUtils;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

@Component
public class QiNiuLogicImpl implements QiNiuLogic {

    @Resource
    private QiNiuOssUtils qiNiuOssUtils;

    @Override
    public R<String> getUploadToken() {
        return R.ok("获取成功", qiNiuOssUtils.getTempToken());
    }
}
