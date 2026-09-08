package com.jiuyu.replay.words.bo.viewing;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(name = "直播作者信息")
public class LiveAuthorBo implements Serializable {
    private static final long serialVersionUID = 1L;


    /**
     * 粉丝总数
     */
   @Schema(description = "粉丝总数")
    private Integer followerCount;

    /**
     * 总获赞数
     */
   @Schema(description = "总获赞数")
    private Integer totalFavorited;

    /**
     * 抖音唯一ID
     */
   @Schema(description = "抖音唯一ID")
    private String uniqueId;

    /**
     * 信誉等级
     */
    @Schema(description = "信誉等级")
    private Byte level;

    /**
     * 信誉分数
     */
   @Schema(description = "信誉分数")
    private Double score;

    /**
     * 信誉百分比
     */
    @Schema(description = "信誉百分比")
    private Double percentage;

    /**
     * 主播唯一ID
     */
   @Schema(description = "主播唯一ID")
    private String authorId;
}
