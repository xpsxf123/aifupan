package com.jiuyu.replay.order.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * 版本基础信息
 *
 * @author lj
 * @email xxxxxxx@qq.com
 * @date 2024-10-08 10:49:32
 */
@Data
@Schema(description = "版本基础信息")
public class VersionBasicInfoBo implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * id
     */
    @Schema(description = "id，有值则为修改，无值则为新增")
    private Long id;

    /**
     * 版本名称
     */
    @Schema(description = "版本名称 [必填]")
    @NotNull(message = "版本名称不能为空")
    private String name;

    /**
     * 等级
     */
    @Schema(description = "等级 [必填]")
    @NotNull(message = "等级不能为空")
    private Integer level;

    /**
     * 版本描述
     */
    @Schema(description = "版本描述 [必填]")
    @NotNull(message = "版本描述不能为空")
    private String description;

    /**
     * 状态 0未上架，1已上架
     */
    @Schema(description = "状态 0未上架，1已上架 [必填]")
    @NotNull(message = "状态不能为空")
    private Integer status;

    /**
     * logo图片列表
     */
    @Schema(description = "logo图片列表 [必填]")
    @NotNull(message = "logo图片不能为空")
    private String logoImgs;
    /**
     * 官网图片
     */
    @Schema(description = "官网图片 [必填]")
    @NotNull(message = "官网logo图片不能为空")
    private String websiteLogoImages;


    @Schema(description = "套餐类型(用户只能购买主要套餐)：1主要套餐，2次要套餐-前端不用传")
    private Integer packageType = 1;

    @Schema(description = "是否循环重置用量(如套餐每个月总送xxx)-前端不用传")
    private Integer resetUse = 1;

    @Schema(description = "重置时间")
    private Integer resetNum = 1;

    @Schema(description = "重置时间单位(0小时，1天，2月，3季度，4半年，5年)-前端不用传")
    private Integer resetUnit = 2;

    @Schema(description = "自定义版本 0：否，1：是")
    private Integer customizeType;
}

