package com.jiuyu.replay.api.logic.third.impl;

import com.jiuyu.replay.api.logic.third.ThirdLogic;
import com.jiuyu.replay.common.alibaba.ImgOssUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.common.utils.RRException;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * @author ：lujie
 * @description：
 * @date ：2025/4/7 下午7:08
 */
@Component
@Slf4j
public class ThirdLogicImpl implements ThirdLogic {

    @Resource
    private ImgOssUtils imgOssUtils;

    /**
     * 图片内容安全检测
     * @param key       图片的key
     * @param type      图片来源类型  0：oss
     * @return
     */
    @Override
    public R<Boolean> imageSecurity(String key, int type) {
        if (type == 0){
            R<Boolean> imaged = imgOssUtils.imageModerationWithOptions(key);
            if (imaged.getCode() != 0){
                log.info("图片内容安全检测失败，删除图片, ossKay={}", key);
                imgOssUtils.deleteObject(key);
                return imaged;
            }else{
                return R.ok(true);
            }
        }
        RRException.create("图片来源未知");
        return null;
    }
}
