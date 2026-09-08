package com.jiuyu.replay.generic.vo.aiagent;

import lombok.Data;

import java.io.Serializable;

/**
 * 达人短视频列表项（仅租户已采集）。
 *
 * @author fupan-server
 */
@Data
public class InfluencerVideoItemVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 视频 id（tb_video_info.id），批量原文接口用它定位
     */
    private Long id;

    /**
     * 平台类型：1-抖音 2-快手 3-视频号
     */
    private Byte platformType;

    /**
     * 平台视频 id
     */
    private String platformVideoId;

    /**
     * 视频文件 hash（MongoDB 内容关联键）
     */
    private String videoHash;

    /**
     * 视频标题
     */
    private String title;

    /**
     * 封面图 URL
     */
    private String coverUrl;

    /**
     * 视频播放 URL
     */
    private String videoUrl;

    /**
     * 作者 id（= 达人 id 字符串）
     */
    private String authorId;

    /**
     * 作者名称
     */
    private String authorName;

    /**
     * 点赞数
     */
    private Long likeCount;

    /**
     * 评论数
     */
    private Long commentCount;

    /**
     * 分享数
     */
    private Long shareCount;

    /**
     * 收藏数
     */
    private Long collectCount;

    /**
     * 时长（秒）
     */
    private Integer duration;

    /**
     * 发布时间（yyyy-MM-dd HH:mm:ss）
     */
    private String publishTime;

    /**
     * 文案提取状态：0-未提取 1-已提取。
     *
     * <p>租户范围查询（collectedOnly=true / 按 id 直查明细）为<b>租户口径</b>：1=该视频在本租户下已提取成功、
     * 可读文案，0=其余（本租户未提取 / 处理中 / 提取失败）；全库爬取库查询取 tb_video_info 原值。</p>
     */
    private Byte extractStatus;

    /**
     * AI 分析状态：0-未分析 1-已分析
     */
    private Byte analysisStatus;

    /**
     * 本租户采集时间（tb_video_user_video.created_date，yyyy-MM-dd HH:mm:ss）
     */
    private String collectTime;

    /**
     * 原文音频转文字内容（未优化）；仅 withAudioText=true 时返回。
     * 租户范围查询下还须该视频在本租户已提取成功，否则为 null；全库爬取库查询不设此限
     */
    private String originalAudioContent;

    /**
     * AI 优化后音频转文字内容；仅 withAudioText=true 时返回。
     * 租户范围查询下还须该视频在本租户已提取成功，否则为 null；全库爬取库查询不设此限
     */
    private String audioContent;
}
