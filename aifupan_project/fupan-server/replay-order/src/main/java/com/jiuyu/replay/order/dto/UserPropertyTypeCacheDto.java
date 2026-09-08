package com.jiuyu.replay.order.dto;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import com.jiuyu.replay.order.vo.UserPropertyInfoVo;
import com.jiuyu.replay.generic.vo.order.UserPropertyTypeInfoVo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * lujie
 * 用户资产类型缓存
 */
@Data
public class UserPropertyTypeCacheDto implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "用户id")
    private Long userId;

    @Schema(description = "父用户id")
    private Long parentUserId;

    @Schema(description = "资产id")
    private Long propertyId;

    @Schema(description = "父资产id")
    private Long parentPropertyId;

    @Schema(description = "ai语音分析时长-剩余")
    private Long aiAnalysisTime = 0L;

    @Schema(description = "ai语音分析时长-已使用")
    private Long useAiAnalysisTime = 0L;

    @Schema(description = "ai语音分析时长-总数")
    private Long totalAiAnalysisTime = 0L;

    @Schema(description = "视频标注时长-剩余")
    private Long videoTaggingTime = 0L;

    @Schema(description = "视频标注时长-已使用")
    private Long useVideoTaggingTime = 0L;

    @Schema(description = "视频标注时长-总数")
    private Long totalVideoTaggingTime = 0L;

    @Schema(description = "文本标注字数-剩余")
    private Long textTaggingWordCount = 0L;

    @Schema(description = "文本标注字数-已使用")
    private Long useTextTaggingWordCount = 0L;

    @Schema(description = "文本标注字数-总数")
    private Long totalTextTaggingWordCount = 0L;

    @Schema(description = "监控数-剩余")
    private Long monitorNum = 0L;

    @Schema(description = "监控数-已使用")
    private Long useMonitorNum = 0L;

    @Schema(description = "监控数-总数")
    private Long totalMonitorNum = 0L;

    @Schema(description = "主播数-剩余")
    private Long anchorNum = 0L;

    @Schema(description = "主播数-已使用")
    private Long useAnchorNum = 0L;

    @Schema(description = "主播数-总数")
    private Long totalAnchorNum = 0L;

    @Schema(description = "存储空间-剩余")
    private Long storageNum = 0L;

    @Schema(description = "存储空间-已使用")
    private Long useStorageNum = 0L;

    @Schema(description = "存储空间-总数")
    private Long totalStorageNum = 0L;

    @Schema(description = "子账号数-剩余")
    private Long subAccountCount = 0L;

    @Schema(description = "子账号数-已使用")
    private Long useSubAccountCount = 0L;

    @Schema(description = "子账号数-总数")
    private Long totalSubAccountCount = 0L;

    @Schema(description = "子账号监控数-剩余")
    private Long child_monitorNum = 0L;

    @Schema(description = "子账号监控数-已使用")
    private Long useChild_monitorNum = 0L;

    @Schema(description = "子账号监控数-总数")
    private Long totalChild_monitorNum = 0L;

    @Schema(description = "子账号主播数-剩余")
    private Long child_anchorNum = 0L;

    @Schema(description = "子账号主播数-已使用")
    private Long useChild_anchorNum = 0L;

    @Schema(description = "子账号主播数-总数")
    private Long totalChild_anchorNum = 0L;

    @Schema(description = "主播弹幕监控位-剩余")
    private Long anchorBarrageNum = 0L;

    @Schema(description = "主播弹幕监控位-已使用")
    private Long useAnchorBarrageNum = 0L;

    @Schema(description = "主播弹幕监控位-总数")
    private Long totalAnchorBarrageNum = 0L;

    @Schema(description = "弹幕显示条数-总数")
    private Long totalBarrageNum = 0L;

    @Schema(description = "AI分析文本数量-剩余")
    private Long aiTokenNum;

    @Schema(description = "AI分析文本数量-已使用")
    private Long useAiTokenNum;

    @Schema(description = "AI分析文本数量-总数")
    private Long totalAiTokenNum;

    @Schema(description = "图片识别数量-剩余")
    private Long imgIdentifyNum;

    @Schema(description = "图片识别数量-已使用")
    private Long useImgIdentifyNum;

    @Schema(description = "图片识别数量-总数")
    private Long totalImgIdentifyNum;

    @Schema(description = "数据看板获取-剩余")
    private Long dataBoardNum;

    @Schema(description = "数据看板-已使用")
    private Long useDataBoardNum;

    @Schema(description = "数据看板-总数")
    private Long totalDataBoardNum;

    @Schema(description = "短信条数-剩余")
    private Long smsMessageNum;

    @Schema(description = "短信条数-已使用")
    private Long useSmsMessageNum;

    @Schema(description = "短信条数-总数")
    private Long totalSmsMessageNum;

    @Schema(description = "文案提取时长-剩余")
    private Long textExtractionNum;

    @Schema(description = "文案提取时长-已使用")
    private Long useTextExtractionNum;

    @Schema(description = "文案提取时长-总数")
    private Long totalTextExtractionNum;

    @Schema(description = "巨量监控位-剩余")
    private Long rpaAmountNum;

    @Schema(description = "巨量监控位-已使用")
    private Long useRpaAmountNum;

    @Schema(description = "巨量监控位-总数")
    private Long totalRpaAmountNum;

    @Schema(description = "快手监控位-剩余")
    private Long kuaishouMonitorNum;

    @Schema(description = "快手监控位-已使用")
    private Long useKuaishouMonitorNum;

    @Schema(description = "快手监控位-总数")
    private Long totalKuaishouMonitorNum;

    @Schema(description = "提取文案条数-剩余")
    private Long shortVideoNum;

    @Schema(description = "提取文案条数-已使用")
    private Long useShortVideoNum;

    @Schema(description = "提取文案条数-总数")
    private Long totalShortVideoNum;

    @Schema(description = "搜索爆款数量-剩余")
    private Long searchHotVideoNum;

    @Schema(description = "搜索爆款数量-已使用")
    private Long useSearchHotVideoNum;

    @Schema(description = "搜索爆款数量-总数")
    private Long totalSearchHotVideoNum;

    @Schema(description = "搜索达人数量-剩余")
    private Long searchInfluencerNum;

    @Schema(description = "搜索达人数量-已使用")
    private Long useSearchInfluencerNum;

    @Schema(description = "搜索达人数量-总数")
    private Long totalSearchInfluencerNum;

    @Schema(description = "订阅爆款数量-剩余")
    private Long subscribeHotVideoNum;

    @Schema(description = "订阅爆款数量-已使用")
    private Long useSubscribeHotVideoNum;

    @Schema(description = "订阅爆款数量-总数")
    private Long totalSubscribeHotVideoNum;

    @Schema(description = "订阅达人数量-剩余")
    private Long subscribeInfluencerNum;

    @Schema(description = "订阅达人数量-已使用")
    private Long useSubscribeInfluencerNum;

    @Schema(description = "订阅达人数量-总数")
    private Long totalSubscribeInfluencerNum;

    @Schema(description = "账号订阅达人数量-剩余")
    private Long userSubscribeInfluencerNum;

    @Schema(description = "账号订阅达人数量-已使用")
    private Long useUserSubscribeInfluencerNum;

    @Schema(description = "账号订阅达人数量-总数")
    private Long totalUserSubscribeInfluencerNum;

    @Schema(description = "账号订阅爆款数量-剩余")
    private Long userSubscribeHotVideoNum;

    @Schema(description = "账号订阅爆款数量-已使用")
    private Long useUserSubscribeHotVideoNum;

    @Schema(description = "账号订阅爆款数量-总数")
    private Long totalUserSubscribeHotVideoNum;

    @Schema(description = "视频号监控位-剩余")
    private Long channelMonitorNum;

    @Schema(description = "视频号监控位-已使用")
    private Long useChannelMonitorNum;

    @Schema(description = "视频号监控位-总数")
    private Long totalChannelMonitorNum;

    @Schema(description = "子公司数量-剩余")
    private Long enterpriseSubsidiariesNum;

    @Schema(description = "子公司数量-已使用")
    private Long useEnterpriseSubsidiariesNum;

    @Schema(description = "子公司数量-总数")
    private Long totalEnterpriseSubsidiariesNum;

    @Schema(description = "人员数量-剩余")
    private Long enterprisePersonNum;

    @Schema(description = "人员数量-已使用")
    private Long useEnterprisePersonNum;

    @Schema(description = "人员数量-总数")
    private Long totalEnterprisePersonNum;

    public UserPropertyTypeCacheDto (){}

    public static UserPropertyTypeCacheDto create (UserPropertyInfoVo userProperty){
        UserPropertyTypeCacheDto cacheDto = new UserPropertyTypeCacheDto();
        Map<String, Long> map = new HashMap<>();
        if (ObjectUtil.isNotEmpty(userProperty)){
            map.put("userId", userProperty.getUserId());
            map.put("parentUserId", userProperty.getParentUserId());
            map.put("propertyId", userProperty.getId());
            map.put("parentPropertyId", userProperty.getParentId());
            if (ObjectUtil.isNotEmpty(userProperty.getUserPropertyTypeList())){
                List<UserPropertyTypeInfoVo> list = userProperty.getUserPropertyTypeList();
                list.forEach(item -> {
                    map.put(item.getCommodityTypeCode(), BigDecimal.valueOf(ObjectUtil.defaultIfNull(item.getTotalQuantity(), 0L))
                            .subtract(BigDecimal.valueOf(ObjectUtil.defaultIfNull(item.getUseQuantity(), 0L))).longValue());
                    String code = Character.toUpperCase(item.getCommodityTypeCode().charAt(0)) + item.getCommodityTypeCode().substring(1);
                    map.put("use"+code, item.getUseQuantity());
                    map.put("total"+code, item.getTotalQuantity());
                });
            }
            BeanUtil.copyProperties(map, cacheDto);
        }

        return cacheDto;
    }

}
