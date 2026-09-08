package com.jiuyu.replay.generic.bo.power;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Update;

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
public class SalesBo implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * id
	 */
	@Schema(description = "id")
    @NotNull(message = "跟进销售人员名不能为空", groups = {Update.class, IsChooseUpdate.class, IsUpdateUserPolling.class})
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
    @NotNull(message = "跟进销售人员名不能为空", groups = {Update.class, Insert.class})
	private String salesName;
	/**
	 * 销售人员手机号
	 */
	@Schema(description = "销售人员手机号")
    @NotNull(message = "销售人员手机号不能为空", groups = {Update.class, Insert.class})
	private String phone;
	/**
	 * 二维码图片文件id
	 */
	@Schema(description = "二维码图片文件id")
    @NotNull(message = "销售人员二维码不能为空")
	private Long qrcodeImgId;
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
	 * 是否开启轮询
	 */
	@Schema(description = "是否开启轮询")
    @NotNull(message = "是否分配线索不能为空", groups = {Update.class, Insert.class, IsChooseUpdate.class})
	private Integer isChoose;

    /**
     * 是否开启用户轮询 0否 1是
     */
    @Schema(description = "是否开启用户轮询 0否 1是")
    @NotNull(message = "是否开启用户轮询不能为空", groups = {Update.class, Insert.class, IsUpdateUserPolling.class})
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


    public static interface IsChooseUpdate {
    }

    public static interface IsUpdateUserPolling {
    }
}


