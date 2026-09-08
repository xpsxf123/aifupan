package com.jiuyu.replay.words.vo;

import com.jiuyu.replay.words.vo.file.UploadFileInfoVo;
import com.jiuyu.replay.generic.vo.words.AnchorVideoInfoVo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 运营/违规收藏列表信息
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-03-12 16:42:18
 */
@Data
@Schema(description = "运营/违规收藏列表信息")
public class ClientAiFavVo implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * ID
	 */
	@Schema(description = "ID")
	private Long id;
	/**
	 * 收藏类型 0：运营助手 1：违规助手
	 */
	@Schema(description = "收藏类型 0：运营助手 1：违规助手")
	private Integer favType;
	/**
	 * 数据来源类型 0：录制视频 1：文件上传 2：对比
	 */
	@Schema(description = "数据来源类型 0：录制视频 1：文件上传 2：对比")
	private Integer dataResourceType;
	/**
	 * 数据来源id
	 */
	@Schema(description = "数据来源id")
	private String dataResourceUuid;
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
	 * 视频信息
	 */
	@Schema(description = "视频信息")
	private AnchorVideoInfoVo videoInfo;
	/**
	 * 文件信息
	 */
	@Schema(description = "文件信息")
	private UploadFileInfoVo fileInfo;
	/**
	 * 对比信息
	 */
	@Schema(description = "对比信息")
	private SyncContrastInfoVo contrastInfo;

}
