package com.jiuyu.replay.api.logic.third;

import com.jiuyu.replay.generic.vo.common.R;

/**
 * @author ：lujie
 * @description：第三方的接口
 * @date ：2025/4/7 下午7:05
 */
public interface ThirdLogic {


    /**
     * 图片内容安全检测
     * @param key       图片的key
     * @param type      图片来源类型  0：oss
     * @return
     */
    R<Boolean> imageSecurity(String key, int type);
}
