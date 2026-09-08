package com.jiuyu.replay.generic.vo.words;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "关键词/敏感词")
public class WordsMarkVo {

    /**
     * 记录要标注的词语位置，比如[0, 2]则表示标注当前段落第0个、第2个词语（用来兼容1.8.3及以前版本）
     */
    @Schema(description = "记录要标注的词语位置，比如[0, 2]则表示标注当前段落第0个、第2个词语（用来兼容1.8.3及以前版本）")
    private List<Integer> recordNeedsWordNumList;

    /**
     * 记录要标注的词语位置，比如[0, 2]则表示标注当前段落第0个、第2个词语
     */
    @Schema(description = "记录要标注的词语位置，比如[0, 2]则表示标注当前段落第0个、第2个词语")
    private List<RecordNeedsWordVo> recordNeedsWordList;

    /**
     * 词语类型  0：敏感词 1：关键词 2：白名单(只有客户自定义有)
     */
    @Schema(description = "词语类型  0：敏感词 1：关键词 2：白名单(只有客户自定义有)")
    private Integer wordsType;
    /**
     * 词语类型
     */
    @Schema(description = "词语类型")
    private String wordsTypeStr;
    /**
     * 来源类型 0：系统 1：客户自定义
     */
    @Schema(description = "来源类型 0：系统 1：客户自定义")
    private Integer resourceType;
    /**
     * 来源类型
     */
    @Schema(description = "来源类型")
    private String resourceTypeStr;
    /**
     * 细分类型
     * 关键词 0：促单 1：互动 2：其他
     * 敏感词 0：广告 1：品牌 2：国家 3：限制词 4：其他
     */
    @Schema(description = "细分类型 关键词 0：促单 1：互动 2：其他   敏感词 0：广告 1：品牌 2：国家 3：限制词 4：其他")
    private Integer type;
    /**
     * 细分类型
     */
    @Schema(description = "细分类型")
    private String typeStr;
    /**
     * 平台类型 0：全平台 1：抖音 2：快手 3：视频号
     */
    @Schema(description = "平台类型 0：全平台 1：抖音 2：快手 3：视频号")
    private Integer platformType;
    /**
     * 平台类型
     */
    @Schema(description = "平台类型")
    private String platformTypeStr;
    /**
     * 关键词名字
     */
    @Schema(description = "关键词名字")
    private String name;
    /**
     * 敏感词等级 0：1级,封号  1：2级,严重警告  2：3级警告
     */
    @Schema(description = "敏感词等级 0：1级,封号  1：2级,严重警告  2：3级警告")
    private Integer level;
    /**
     * 敏感词等级
     */
    @Schema(description = "敏感词等级")
    private String levelStr;
    /**
     * 需要标注的次数
     */
    @Schema(description = "需要标注的次数")
    private Integer countNum;
    /**
     * 描述
     */
    @Schema(description = "描述")
    private String remarks;
    /**
     * 行业id，1表示全行业
     */
    @Schema(description = "行业id，1表示全行业")
    private Long tradeId;
    /**
     * 行业
     */
    @Schema(description = "行业")
    private String tradeStr;
    /**
     * 行业id数组
     */
    @Schema(description = "行业id数组")
    private String tradeIdArr;
    /**
     * 分组
     */
    @Schema(description = "分组")
    private String groupStr;
    /**
     * 关键词类型id
     */
    @Schema(description = "关键词类型id")
    private Long cruxTypeId;
    /**
     * 关键词类型信息
     */
    @Schema(description = "关键词类型信息")
    private CruxTypeInfoVo cruxTypeInfo;
    /**
     * 词语在文中出现的总次数
     */
    @Schema(description = "词语在文中出现的总次数")
    private Integer totalNum;
    /**
     * 概览
     */
    @Schema(description = "概览")
    private String overView;
}
