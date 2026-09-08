package com.jiuyu.replay.words.producer;

import com.jiuyu.replay.generic.vo.words.VideoContentVo;

import java.util.List;

/**
 * 自然、优化原文的内容存储
 */
public interface VideoContentProducer {


    /**
     * 更具索引获取数据
     *
     * @param sourceId
     * @param type
     * @param userId
     * @param tenantId
     * @param generateStatus
     * @return
     */
    List<VideoContentVo> listBySourceIdAndStatus(String sourceId, Integer type, Long userId, Long tenantId, int generateStatus);

    /**
     * 批量保存
     *
     * @param voList
     * @return
     */
    List<VideoContentVo> saveAll(List<VideoContentVo> voList);

    /**
     * 根据id修改
     *
     * @param contentVo
     */
    void updateById(VideoContentVo contentVo);

    /**
     * 格式化内容：按换行分割 → 过滤空行 → 去首尾空格 → 过滤纯标点符号行 → 转 JSON 数组字符串
     *
     * @param content 原始内容
     * @return 格式化后的 JSON 数组字符串，内容为空时返回原值
     */
    String formatContent(String content);

    /**
     * 检测是否还有提示词没有完成
     *
     * @param sourceId
     * @param type
     * @param userId
     * @param tenantId
     * @return
     */
    boolean checkContentAllComplete(String sourceId, Integer type, Long userId, Long tenantId);
}
