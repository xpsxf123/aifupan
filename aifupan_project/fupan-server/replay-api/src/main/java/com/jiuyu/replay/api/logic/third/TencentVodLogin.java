package com.jiuyu.replay.api.logic.third;

import com.jiuyu.replay.generic.vo.common.R;

public interface TencentVodLogin {

    /**
     * 获取vod上传签名
     * @return
     */
    R<String> getVodUploadSign();
}
