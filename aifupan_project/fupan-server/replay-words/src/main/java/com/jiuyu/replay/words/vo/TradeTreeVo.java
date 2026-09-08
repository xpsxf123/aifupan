package com.jiuyu.replay.words.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

@Data
@Schema(description = "行业信息")
public class TradeTreeVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * ID
     */
    @Schema(description = "ID")
    private Long id;
    /**
     * 行业名称
     */
    @Schema(description = "行业名称")
    private String name;
    /**
     * 行业描述
     */
    @Schema(description = "行业描述")
    private String remarks;
    /**
     * 默认的通用模型id
     */
    @Schema(description = "默认的通用模型id")
    private Long defaultGeneralModelId;
    /**
     * 行业模型id，为0表示没有
     */
    @Schema(description = "行业模型id，为0表示没有")
    private Long tradeModelId;
    /**
     * 父行业ID
     */
    @Schema(description = "父行业ID")
    private Long parentId;
    /**
     * 排序
     */
    @Schema(description = "排序")
    private Integer sort;

    /**
     * 行业热榜生效状态（0:不生效, 1:生效）
     */
    private Integer rankEnabled;

    /**
     * 是否是行业热榜有效行业（0:不是, 1:是）
     * 只有在有效的叶子节点行业列表中的行业才返回1
     */
    @Schema(description = "是否是行业热榜有效行业（0:不是, 1:是）")
    private Integer isValidRankTrade;

    /**
     * 子行业
     */
    @Schema(description = "子行业")
    private List<TradeTreeVo> children;
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
     * 关键词数量
     */
    @Schema(description = "关键词数量")
    private Integer cruxNum;
    /**
     * 敏感词数量
     */
    @Schema(description = "敏感词数量")
    private Integer sensitiveNum;
    /**
     * 提示词数量
     */
    @Schema(description = "提示词数量")
    private Integer cueNum;
    /**
     * 运营全文提示词数量
     */
    @Schema(description = "运营全文提示词数量")
    private Integer opeFullcueNum;
    /**
     * 运营段落提示词数量
     */
    @Schema(description = "运营段落提示词数量")
    private Integer opeParCueNum;
    /**
     * 违规全文提示词数量
     */
    @Schema(description = "违规全文提示词数量")
    private Integer vioFullCueNum;
    /**
     * 违规段落提示词数量
     */
    @Schema(description = "违规段落提示词数量")
    private Integer vioParCueNum;

    /**
     * 行业热榜数量
     */
    @Schema(description = "行业热榜数量")
    private Long tradeRankCount;

    /**
     * 抖音主播数量
     */
    @Schema(description = "抖音主播数量")
    private Integer douyinAnchorCount;

    /**
     * 快手主播数量
     */
    @Schema(description = "快手主播数量")
    private Integer kuaishouAnchorCount;

    /**
     * 视频号主播数量
     */
    @Schema(description = "视频号主播数量")
    private Integer shipinhaoAnchorCount;

}
