package com.jiuyu.replay.words.vo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class AnchorClientVo {

    /**
     * ID
     */
    @TableId(value = "id", type = IdType.INPUT)
    @Schema(description = "ID")
    private Long id;
    /**
     * 主播唯一标识
     */
    @Schema(description = "主播唯一标识")
    private String secUid;
    /**
     * 主页url
     */
    @Schema(description = "主页url")
    private String homeUrl;
    /**
     * 直播间url
     */
    @Schema(description = "直播间url")
    private String liveUrl;
    /**
     * 主播名称
     */
    @Schema(description = "主播名称")
    private String anchorName;
    /**
     * 主播头像
     */
    @Schema(description = "主播头像")
    private String anchorAvatar;
    /**
     * 平台类型 0：抖音 1：快手 2：视频号
     */
    @Schema(description = "平台类型 0：抖音 1：快手 2：视频号")
    private Integer platform;
    /**
     * DouYinHomeLive(个人主页地址),DouYinLive（直播地址） 抖音直播 TiktokLive 国外版本抖音  KuaiShouLive 快手   其他待定
     */
    @Schema(description = "DouYinHomeLive(个人主页地址),DouYinLive（直播地址） 抖音直播 TiktokLive 国外版本抖音  KuaiShouLive 快手   其他待定")
    private String platformResource;
    /**
     * 主播userId
     */
    @Schema(description = "主播userId")
    private String anchorUserId;
    /**
     * webSocketId
     */
    @Schema(description = "webSocketId")
    private String webSocketId;
    /**
     * 主播抖音号
     */
    @Schema(description = "主播抖音号")
    private String anchorNumber;
    /**
     * 第三方数据平台是否已经收录 0:否 1:是
     */
    @Schema(description = "第三方数据平台是否已经收录 0:否 1:是")
    private Integer chanmamaInclude;
    /**
     * 系统行业id
     */
    @Schema(description = "系统行业id")
    private Long systemTradeId;
    /**
     * AI纠正后的行业id
     */
    @Schema(description = "AI纠正后的行业id")
    private Long aiCorrectTradeId;
    /**
     * 在线时,是否自动录制分析
     */
    @Schema(description = "在线时,是否自动录制分析")
    private Integer isAutoRecord;
    /**
     * 是否从录制列表移除了 0：否 1：是 2：已从恢复列表删除
     */
    @Schema(description = "是否从录制列表移除了 0：否 1：是 2：已从恢复列表删除")
    private Integer isRemoveRecord;
    /**
     * 是否开启弹幕监控 0否， 1是
     */
    @Schema(description = "是否开启弹幕监控 0否， 1是")
    private Integer isBarrageMonitoring;
    /**
     * 是否自动上传到云空间 0：否 1：是
     */
    @Schema(description = "是否自动上传到云空间 0：否 1：是")
    private Integer isAutoUploadCloud;
    /**
     * 是否置顶 0：否 1：是
     */
    @Schema(description = "是否置顶 0：否 1：是")
    private Integer isTop;
    /**
     * 加入置顶的时间
     */
    @Schema(description = "加入置顶的时间")
    private String addTopTime;
    /**
     * 最后开始录制时间
     */
    @Schema(description = "最后开始录制时间")
    private String lastRecordTime;
    /**
     * 主播账号情况描述
     */
    @Schema(description = "主播账号情况描述")
    private String anchorSituation;
    /**
     * 录制时间，如：06:00:00-19:00:00
     */
    @Schema(description = "录制时间，如：06:00:00-19:00:00")
    private String recordTime;
    /**
     * 上下播短信提醒 0：不提醒 1：上播提醒 2：下播提醒 3：上下播提醒
     */
    @Schema(description = "上下播短信提醒 0：不提醒 1：上播提醒 2：下播提醒 3：上下播提醒")
    private Integer smsTip;
    /**
     * 是否开启数据看板 0：否 1：是
     */
    @Schema(description = "是否开启数据看板 0：否 1：是")
    private Integer isDataViewing;
    /**
     * 账号归属类型 0：自由账号 1：同行账号
     */
    @Schema(description = "账号归属类型 0：自由账号 1：同行账号")
    private Integer accountType;
    /**
     * 文件夹名称(主播名去掉特殊符号，如果只有特殊符号用uuid)
     */
    @Schema(description = "文件夹名称(主播名去掉特殊符号，如果只有特殊符号用uuid)")
    private String folderName;
    /**
     * 主播备注名称
     */
    @Schema(description = "主播备注名称")
    private String remarksName;
    /**
     * 行业id
     */
    @Schema(description = "行业id")
    private Long tradeId;
    /**
     * 租户id
     */
    @Schema(description = "租户id")
    private Long tenantId;
    /**
     * 录制时间模式 0：按视频时长录制 1：按北京时间录制（默认） 2：按时间点分段录制
     */
    @Schema(description = "录制时间模式 0：按视频时长录制 1：按北京时间录制（默认） 2：按时间点分段录制")
    private Integer recordTimeMode;
    /**
     * 时间点分段录制的时间点列表，格式："09:30,10:40,11:20"，多个时间点用逗号分隔
     */
    @Schema(description = "时间点分段录制的时间点列表，格式：09:30,10:40,11:20，多个时间点用逗号分隔")
    private String segmentTimePoints;

}
