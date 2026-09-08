package com.jiuyu.replay.words.vo;

import com.jiuyu.replay.generic.vo.words.AnchorUrlInfoVo;
import com.jiuyu.replay.generic.vo.words.AnchorVideoInfoVo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 客户端对比数据列表项
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-09-26 14:51:59
 */
@Data
@Schema(description = "客户端对比数据列表项")
public class SyncContrastListVo extends SyncContrastVo implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * 主播一信息
	 */
	@Schema(description = "主播一信息")
	private AnchorUrlInfoVo anchorInfoOne;
	/**
	 * 主播二信息
	 */
	@Schema(description = "主播二信息")
	private AnchorUrlInfoVo anchorInfoTwo;
	/**
	 * 视频一信息
	 */
	@Schema(description = "视频一信息")
	private AnchorVideoInfoVo videoInfoOne;
	/**
	 * 视频二信息
	 */
	@Schema(description = "视频二信息")
	private AnchorVideoInfoVo videoInfoTwo;
	/**
	 * 上传用户的名称
	 */
	@Schema(description = "上传用户的名称")
	private String userNickName;

	/**
	 * 视频一名称
	 */
	@Schema(description = "视频一名称")
	private String videoNickNameOne;
	/**
	 * 视频二名称
	 */
	@Schema(description = "视频二名称")
	private String videoNickNameTwo;
	/**
	 * 视频一行业
	 */
	@Schema(description = "视频一行业")
	private String tradeNickNameOne;
	/**
	 * 视频二行业
	 */
	@Schema(description = "视频二行业")
	private String tradeNickNameTwo;    /**
	 * 用户销售人员
	 */
	@Schema(description = "用户销售人员")
	private String userSales;

    @Schema(description = "用户销售人员id")
    private Long salesId;

    @Schema(description = "版本名称")
    private String packageName;

    @Schema(description = "版本过期时间")
    private Date packageExpiredTime;
}
