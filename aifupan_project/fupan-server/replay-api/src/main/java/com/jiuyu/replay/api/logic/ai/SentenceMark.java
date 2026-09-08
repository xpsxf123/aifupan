package com.jiuyu.replay.api.logic.ai;

import com.jiuyu.replay.words.bo.AskRequestBo;

import java.util.List;
import java.util.Map;

/**
 * @author ：lujie
 * @description：获取视频、文件、对比分析的数据
 * @date ：2025/3/22 下午7:05
 */
public interface SentenceMark {

    /**
     * 初始化数据
     *
     * @param sourceId
     * @param askType
     * @param videoTimeOneList
     * @param videoTimeTwoList
     */
    void init(String sourceId, Integer askType, List<Long> videoTimeOneList, List<Long> videoTimeTwoList);

    /**
     * 设置对比分析的提问单次最大字数
     * @param otherParams
     */
    void setOtherParams(Map<String, Object> otherParams);

    /**
     * 获取redisKey
     */
    String getContextRedisKey(AskRequestBo askRequestBo);

    /**
     * 获取数据
     * @return
     */
    Object getMarkDto();

    /**
     * 获取全文数据
     * @return
     */
    String getContent();

    /**
     * 获取段落说明
     * @return
     */
    String getTextParamsContent();

    /**
     * 获取全部的内容
     * @return
     */
    String getAllContent();

    /**
     * 获取行业id
     *
     * @return
     */
    Long getTradeId();

    /**
     * 获取行业名称
     * @return
     */
    String getTradeName();

    /**
     * 获取主播名称
     * @return
     */
    String getAnchorName();

    /**
     * 获取数据截图
     * @return
     */
    String getDataScreenshot();

    /**
     * 获取数据看板
     * @return
     */
    String getBoard();

    /**
     * 获取额外要求
     * @return
     */
    List<String> getAdditionalList();

    /**
     * 设置提问的问题
     *
     * @param askRequestBo
     * @return
     */
    String setAskQuestion(AskRequestBo askRequestBo);

    /**
     * 获取平台
     *
     * @return
     */
    String getPlatform();

    /**
     * 获取速度
     *
     * @param askRequestBo 参数
     */
    Integer getSpeed(AskRequestBo askRequestBo);

    /**
     * 设置语速
     *
     * @param askRequestBo 参数
     */
    void setSpeed(AskRequestBo askRequestBo);
}
