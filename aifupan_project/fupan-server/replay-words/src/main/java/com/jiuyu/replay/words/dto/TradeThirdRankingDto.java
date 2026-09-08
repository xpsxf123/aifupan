package com.jiuyu.replay.words.dto;

import com.baomidou.mybatisplus.annotation.TableField;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.Length;
import org.hibernate.validator.constraints.Range;

/**
 * 行业第三方榜单
 *
 * @author HeHui
 * @date 2026-01-28 16:16
 */
@Getter
@Setter
public class TradeThirdRankingDto {

    /**
     * 主键ID 修改时必传
     */
    private Long id;

    /**
     * 行业ID
     */
    @NotNull(message = "所属行业不能为空")
    private Long tradeId;

    /**
     * 第三方榜单唯一ID（第三方平台的榜单ID）
     */
    @NotBlank(message = "第三方榜单ID不能为空")
    @Length(max = 64, message = "第三方榜单ID长度不能超过64")
    private String thirdRankingId;

    /**
     * 第三方榜单名称
     */
    @NotBlank(message = "第三方榜单名称不能为空")
    @Length(max = 100, message = "第三方榜单名称长度不能超过100")
    private String thirdRankingName;


    /**
     * 是否采集
     */
    private Boolean enableCollect;

    /**
     * 每个周期的周几采集 1-7
     */
    @NotNull(message = "每个周期的周几不能为空")
    @Range(min = 1, max = 7, message = "每个周的周几只能是1-7")
    private Integer dayOfWeek;
}
