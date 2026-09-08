package com.jiuyu.replay.generic.vo.power;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 用户跟进销售人员表信息
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-01-21 10:16:13
 */
@Data
@Schema(description = "用户跟进销售人员表信息")
public class SalesVo implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * id
	 */
	@Schema(description = "id")
	private Long id;
	/**
	 * 父ID
	 */
	@Schema(description = "父ID")
	private Long parentId;
	/**
	 * 跟进销售人员名
	 */
	@Schema(description = "跟进销售人员名")
	private String salesName;
	/**
	 * 二维码图片文件id
	 */
	@Schema(description = "二维码图片文件id")
	private Long qrcodeImgId;
	/**
	 * 销售人员手机号
	 */
	@Schema(description = "销售人员手机号")
	private String phone;
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
	@Schema(description = "是否已删除")
	private Integer isDeleted;
	/**
     * 是否分配线索
	 */
    @Schema(description = "是否分配线索 0不开  1开")
	private Integer isChoose;

    /**
     * 是否开启用户轮询 0否 1是
     */
    @Schema(description = "是否开启用户轮询 0否 1是")
    private Integer userPolling;

    /**
     * 获客助手连接
     */
    @Schema(description = "获客助手连接")
    private String salesIntroductionUrl;

    /**
     * 销售类型 0平台销售，1代理商销售
     */
    @Schema(description = "销售类型 0平台销售，1代理商销售")
    private Integer salesType;

    /**
     * 代理商id
     */
    @Schema(description = "代理商id")
    private Long agentId;

    /**
     * 用户id
     */
    @Schema(description = "用户id")
    private Long userId;

    /**
     * 员工状态 0离职  1在职
     */
    @Schema(description = "员工状态 0离职  1在职")
    private Integer employeeStatus;
}
