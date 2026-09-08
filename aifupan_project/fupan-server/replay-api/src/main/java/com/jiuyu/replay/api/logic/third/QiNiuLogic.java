package com.jiuyu.replay.api.logic.third;

import com.jiuyu.replay.generic.vo.common.R;

public interface QiNiuLogic {

    /**
     * 获取上传文件的临时凭证
     * @return
     */
    R<String> getUploadToken();
}
