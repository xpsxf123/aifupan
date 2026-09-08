package com.jiuyu.replay.power.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 用户备注表信息
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-02-12 15:59:28
 */
@Data
@Schema(description = "用户备注表信息")
public class UserRemarkVo implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * ID
	 */
	@Schema(description = "ID")
	private Long id;
	/**
	 * 用户ID
	 */
	@Schema(description = "用户ID")
	private Long userId;
	/**
	 * 用户备注
	 */
	@Schema(description = "用户备注")
	private String remark;
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
	 * 跟进类型
	 */
	@Schema(description = "跟进类型")
	private Integer followType;
    /**
     * 跟进状态
     */
    @Schema(description = "跟进状态")
    private Integer followStatus;
    /**
     * 跟进状态名称
     */
    @Schema(description = "跟进状态名称")
    private String followStatusName;
	/**
	 * 跟进类型名称
	 */
	@Schema(description = "跟进类型名称")
	private String followTypeName;
	/**
	 * 下次跟进时间
	 */
	@Schema(description = "下次跟进时间")
	private Date nextFolTime;
	/**
	 * 创建者 ID
	 */
	@Schema(description = "创建者 ID")
	private Long createId;
	/**
	 * 修改者 ID
	 */
	@Schema(description = "修改者 ID")
	private Long updateId;
	/**
	 * 创建者名称
	 */
	@Schema(description = "创建者名称")
	private String createName;
	/**
	 * 修改者名称
	 */
	@Schema(description = "修改者名称")
	private String updateName;

    /**
     * 跟进时间
     */
    @Schema(description = "跟进时间")
    private Date followUpTime;


}
