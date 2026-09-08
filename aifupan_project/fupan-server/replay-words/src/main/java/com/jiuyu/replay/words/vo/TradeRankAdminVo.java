package com.jiuyu.replay.words.vo;

import com.baomidou.mybatisplus.annotation.TableField;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Date;

/**
 * 行业热榜管理端VO（返回tb_similar_anchor所有字段）
 *
 * @author RayChou
 * @date 2025-10-28
 * @description 后台管理系统行业热榜分页查询返回对象，包含相似主播表所有字段
 */
@Data
@Schema(description = "行业热榜管理端VO")
public class TradeRankAdminVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * ID（tb_similar_anchor主键）
     */
    @Schema(description = "ID（tb_similar_anchor主键）")
    private Long id;

    /**
     * 相似主播榜单ID（tb_hot_search_trade_ranking_list）
     */
    @Schema(description = "相似主播榜单ID（tb_hot_search_trade_ranking_list主键）")
    private Long similarCollectId;

    /**
     * 相似度
     */
    @Schema(description = "相似度")
    private String similarScore;

    /**
     * 平均UV价值
     */
    @Schema(description = "平均UV价值")
    private String liveAverageUv;

    /**
     * 粉丝数
     */
    @Schema(description = "粉丝数")
    private String followerCount;

    /**
     * 平均场观
     */
    @Schema(description = "平均场观")
    private String liveAverageUser;

    /**
     * 场均销售额
     */
    @Schema(description = "场均销售额")
    private String liveAverageAmount;

    /**
     * 直播销售总额
     */
    @Schema(description = "直播销售总额")
    private String totalAmount;

    /**
     * 销售指数
     */
    @Schema(description = "销售指数")
    private String liveTotalAmountCmmInd;

    /**
     * 直播场次
     */
    @Schema(description = "直播场次")
    private Integer liveCount;

    /**
     * 平均停留时长
     */
    @Schema(description = "平均停留时长")
    private String liveAverageOnline;

    /**
     * 蝉妈妈唯一账号ID
     */
    @Schema(description = "蝉妈妈唯一账号ID")
    private String authorId;

    /**
     * 主播抖音账号
     */
    @Schema(description = "主播抖音账号")
    private String uniqueId;

    /**
     * 主播secUid
     */
    @Schema(description = "主播secUid")
    private String secUid;

    /**
     * 采集时间
     */
    @Schema(description = "采集时间")
    private Date collectDate;

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
     * 账号热度（整数）
     */
    @Schema(description = "账号热度（整数）")
    private Integer accountHeat;

    /**
     * 创建时间
     */
    @Schema(description = "创建时间")
    private Date createDate;

    /**
     * 最后修改时间
     */
    @Schema(description = "最后修改时间")
    private Date updateDate;

    /**
     * 是否已删除
     */
    @Schema(description = "是否已删除（0：否 1：是）")
    private Integer isDeleted;

    /**
     * 总销售额（数值，单位：元）- 解析total_amount字段得到的最低销售额数值
     */
    private Long totalAmountNumeric;

    /**
     * 数据来源：1-系统主播相似达人，2-后台录入，3-第三方榜单
     */
    private Integer sourceType;

    /**
     * 采集状态：0-待采集，1-采集中（仅针对系统主播相似达人），2-采集完成
     */
    private Integer collectStatus;

    /**
     * 是否上榜
     */
    private Boolean upRanking;

    /**
     * 关联主播的系统行业ID（取自tb_anchor_url）
     */
    private Long systemTradeId;

    /**
     * 主播secUid
     */
    private String systemSecUid;

    /**
     * 系统行业名称
     */
    private String systemTradeName;

    /**
     * 最后更改系统行业时间
     */
    private LocalDateTime lastUpdateSystemTradeTime;


    /**
     * 是否在库
     */
    private Boolean inStock;

    /**
     * 关联tb_anchor_url的系统主播ID
     */
    private Long systemAnchorId;


    /**
     * 前置账户ID
     */
    private Long preAnchorId;

    /**
     * 前置账户名称
     */
    private String systemAnchorName;

    /**
     * 前置账户头像
     */
    private String systemAnchorAvatar;

    /**
     * 前置账户抖音号
     */
    private String systemAnchorNumber;

    /**
     * 前置账户行业ID
     */
    private Long systemAnchorTradeId;

    /**
     * 前置账户行业名称
     */
    private String systemAnchorTradeName;

    /**
     * 前置账户直播间关键词
     */
    private String systemAnchorKeywords;


    /**
     * 获取是否在库
     *
     */
    public Boolean getInStock() {
        if (inStock == null) {
            this.inStock = systemAnchorId != null && systemAnchorId > 0;
        }
        return inStock;
    }
}

