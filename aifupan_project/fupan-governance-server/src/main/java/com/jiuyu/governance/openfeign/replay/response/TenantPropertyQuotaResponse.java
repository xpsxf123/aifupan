package com.jiuyu.governance.openfeign.replay.response;

import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;

/**
 * 租户资产额度
 */
@Getter
@Setter
public class TenantPropertyQuotaResponse implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主账户
     */
    private Long userId;

    /**
     * 父用户id （基本是null）
     */
    private Long parentUserId;

    /**
     * 资产id 当前使用的资产ID
     */
    private Long propertyId;

    /**
     * 父资产id
     */
    private Long parentPropertyId;


    /**
     * ai语音分析时长-剩余
     */
    private Long aiAnalysisTime = 0L;

    /**
     * ai语音分析时长-已使用
     */
    private Long useAiAnalysisTime = 0L;

    /**
     * ai语音分析时长-总数
     */
    private Long totalAiAnalysisTime = 0L;

    /**
     * 视频标注时长-剩余
     */
    private Long videoTaggingTime = 0L;

    /**
     * 视频标注时长-已使用
     */
    private Long useVideoTaggingTime = 0L;

    /**
     * 视频标注时长-总数
     */
    private Long totalVideoTaggingTime = 0L;

    /**
     * 文本标注字数-剩余
     */
    private Long textTaggingWordCount = 0L;

    /**
     * 文本标注字数-已使用
     */
    private Long useTextTaggingWordCount = 0L;

    /**
     * 文本标注字数-总数
     */
    private Long totalTextTaggingWordCount = 0L;

    /**
     * 监控数-剩余
     */
    private Long monitorNum = 0L;

    /**
     * 监控数-已使用
     */
    private Long useMonitorNum = 0L;

    /**
     * 监控数-总数
     */
    private Long totalMonitorNum = 0L;

    /**
     * 主播数-剩余
     */
    private Long anchorNum = 0L;

    /**
     * 主播数-已使用
     */
    private Long useAnchorNum = 0L;

    /**
     * 主播数-总数
     */
    private Long totalAnchorNum = 0L;

    /**
     * 存储空间-剩余
     */
    private Long storageNum = 0L;

    /**
     * 存储空间-已使用
     */
    private Long useStorageNum = 0L;

    /**
     * 存储空间-总数
     */
    private Long totalStorageNum = 0L;

    /**
     * 子账号数-剩余
     */
    private Long subAccountCount = 0L;

    /**
     * 子账号数-已使用
     */
    private Long useSubAccountCount = 0L;

    /**
     * 子账号数-总数
     */
    private Long totalSubAccountCount = 0L;

    /**
     * 子账号监控数-剩余
     */
    private Long child_monitorNum = 0L;

    /**
     * 子账号监控数-已使用
     */
    private Long useChild_monitorNum = 0L;

    /**
     * 子账号监控数-总数
     */
    private Long totalChild_monitorNum = 0L;

    /**
     * 子账号主播数-剩余
     */
    private Long child_anchorNum = 0L;

    /**
     * 子账号主播数-已使用
     */
    private Long useChild_anchorNum = 0L;

    /**
     * 子账号主播数-总数
     */
    private Long totalChild_anchorNum = 0L;

    /**
     * 主播弹幕监控位-剩余
     */
    private Long anchorBarrageNum = 0L;

    /**
     * 主播弹幕监控位-已使用
     */
    private Long useAnchorBarrageNum = 0L;

    /**
     * 主播弹幕监控位-总数
     */
    private Long totalAnchorBarrageNum = 0L;

    /**
     * 弹幕显示条数-总数
     */
    private Long totalBarrageNum = 0L;

    /**
     * AI分析文本数量-剩余
     */
    private Long aiTokenNum;

    /**
     * AI分析文本数量-已使用
     */
    private Long useAiTokenNum;

    /**
     * AI分析文本数量-总数
     */
    private Long totalAiTokenNum;

    /**
     * 图片识别数量-剩余
     */
    private Long imgIdentifyNum;

    /**
     * 图片识别数量-已使用
     */
    private Long useImgIdentifyNum;

    /**
     * 图片识别数量-总数
     */
    private Long totalImgIdentifyNum;

    /**
     * 数据看板获取-剩余
     */
    private Long dataBoardNum;

    /**
     * 数据看板-已使用
     */
    private Long useDataBoardNum;

    /**
     * 数据看板-总数
     */
    private Long totalDataBoardNum;

    /**
     * 短信条数-剩余
     */
    private Long smsMessageNum;

    /**
     * 短信条数-已使用
     */
    private Long useSmsMessageNum;

    /**
     * 短信条数-总数
     */
    private Long totalSmsMessageNum;

    /**
     * 文案提取时长-剩余
     */
    private Long textExtractionNum;

    /**
     * 文案提取时长-已使用
     */
    private Long useTextExtractionNum;

    /**
     * 文案提取时长-总数
     */
    private Long totalTextExtractionNum;

    /**
     * 巨量监控位-剩余
     */
    private Long rpaAmountNum;

    /**
     * 巨量监控位-已使用
     */
    private Long useRpaAmountNum;

    /**
     * 巨量监控位-总数
     */
    private Long totalRpaAmountNum;

    /**
     * 快手监控位-剩余
     */
    private Long kuaishouMonitorNum;

    /**
     * 快手监控位-已使用
     */
    private Long useKuaishouMonitorNum;

    /**
     * 快手监控位-总数
     */
    private Long totalKuaishouMonitorNum;

    /**
     * 提取文案条数-剩余
     */
    private Long shortVideoNum;

    /**
     * 提取文案条数-已使用
     */
    private Long useShortVideoNum;

    /**
     * 提取文案条数-总数
     */
    private Long totalShortVideoNum;

    /**
     * 搜索爆款数量-剩余
     */
    private Long searchHotVideoNum;

    /**
     * 搜索爆款数量-已使用
     */
    private Long useSearchHotVideoNum;

    /**
     * 搜索爆款数量-总数
     */
    private Long totalSearchHotVideoNum;

    /**
     * 搜索达人数量-剩余
     */
    private Long searchInfluencerNum;

    /**
     * 搜索达人数量-已使用
     */
    private Long useSearchInfluencerNum;

    /**
     * 搜索达人数量-总数
     */
    private Long totalSearchInfluencerNum;

    /**
     * 订阅爆款数量-剩余
     */
    private Long subscribeHotVideoNum;

    /**
     * 订阅爆款数量-已使用
     */
    private Long useSubscribeHotVideoNum;

    /**
     * 订阅爆款数量-总数
     */
    private Long totalSubscribeHotVideoNum;

    /**
     * 订阅达人数量-剩余
     */
    private Long subscribeInfluencerNum;

    /**
     * 订阅达人数量-已使用
     */
    private Long useSubscribeInfluencerNum;

    /**
     * 订阅达人数量-总数
     */
    private Long totalSubscribeInfluencerNum;

    /**
     * 账号订阅达人数量-剩余
     */
    private Long userSubscribeInfluencerNum;

    /**
     * 账号订阅达人数量-已使用
     */
    private Long useUserSubscribeInfluencerNum;

    /**
     * 账号订阅达人数量-总数
     */
    private Long totalUserSubscribeInfluencerNum;

    /**
     * 账号订阅爆款数量-剩余
     */
    private Long userSubscribeHotVideoNum;

    /**
     * 账号订阅爆款数量-已使用
     */
    private Long useUserSubscribeHotVideoNum;

    /**
     * 账号订阅爆款数量-总数
     */
    private Long totalUserSubscribeHotVideoNum;

    /**
     * 视频号监控位-剩余
     */
    private Long channelMonitorNum;

    /**
     * 视频号监控位-已使用
     */
    private Long useChannelMonitorNum;

    /**
     * 视频号监控位-总数
     */
    private Long totalChannelMonitorNum;

    /**
     * 子公司数量-剩余
     */
    private Long enterpriseSubsidiariesNum;

    /**
     * 子公司数量-已使用
     */
    private Long useEnterpriseSubsidiariesNum;

    /**
     * 子公司数量-总数
     */
    private Long totalEnterpriseSubsidiariesNum;

    /**
     * 人员数量-剩余
     */
    private Long enterprisePersonNum;

    /**
     * 人员数量-已使用
     */
    private Long useEnterprisePersonNum;

    /**
     * 人员数量-总数
     */
    private Long totalEnterprisePersonNum;


}
