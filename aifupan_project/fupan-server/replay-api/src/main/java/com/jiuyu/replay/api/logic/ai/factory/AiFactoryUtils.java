package com.jiuyu.replay.api.logic.ai.factory;

import cn.hutool.core.util.StrUtil;
import com.jiuyu.replay.api.logic.ai.SentenceMark;
import com.jiuyu.replay.common.utils.ApplicationContextUtil;
import com.jiuyu.replay.common.utils.RRException;

import java.util.Map;

/**
 * @author ：lujie
 * @description：
 * @date ：2025/3/23 下午4:52
 */
public class AiFactoryUtils {

    /**
     * 根据sourceType和askType获取对应的SentenceMark
     * @param sourceType        0视频，1文件，2对比分析
     * @param askType           0运营助手 1违规助手 2弹幕助手 3数据截图助手 4数据看板助手
     * @return
     */
    public static SentenceMark getSentenceMark(Integer sourceType, Integer askType){
        Map<String, String> map = Map.of(
                // 视频相关的助手
                "0", "videoSentenceImpl",// 运营、违规助手
                "0-2", "barrageSentenceImpl",// 弹幕助手
                "0-3", "screenshotVideoSentenceImpl",// 截图助手
                "0-4", "viewingConfuseSentenceImpl",// 看板助手
                "0-6", "videoSentenceImpl",// ai话术助手

                // 文件相关的助手
                "1", "fileSentenceImpl",// 运营助手
                "1-3", "screenshotFileSentenceImpl", // 截图助手

                // 对比相关的助手
                "2", "syncContrastSentenceImpl", // 运营助手
                "2-3", "screenshotContrastSentenceImpl",// 截图助手
                "2-4", "viewingContrastSentenceImpl"// 看板助手
        );

        // 获取对应的service Code
        String code = StrUtil.format("{}-{}", sourceType, askType);
        String serviceCode = map.get(code);


        // 如果没有获取到就使用默认的serviceCode
        if (StrUtil.isEmpty(serviceCode)) serviceCode = map.get(sourceType.toString());

        RRException.isNotEmpty(serviceCode, "助手类型不支持");

        SentenceMark bean = (SentenceMark) ApplicationContextUtil.getBean(serviceCode);
        RRException.isNotEmpty(bean, "获取SentenceMark失败");
        return bean;
    }

}
