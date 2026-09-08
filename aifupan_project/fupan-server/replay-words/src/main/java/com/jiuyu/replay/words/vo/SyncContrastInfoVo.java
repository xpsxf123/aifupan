package com.jiuyu.replay.words.vo;


import com.jiuyu.replay.generic.vo.words.AnchorUrlInfoVo;
import com.jiuyu.replay.generic.vo.words.SourceStarVo;
import com.jiuyu.replay.words.vo.file.UploadFileInfoVo;
import com.jiuyu.replay.generic.vo.words.AnchorVideoInfoVo;
import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;

/**
 * 客户端对比数据信息项
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-09-26 14:51:59
 */
@Data
@Schema(description = "客户端对比数据信息项")
public class SyncContrastInfoVo extends SyncContrastVo implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * 视频信息1
	 */
	@Schema(description = "视频信息1")
	private AnchorVideoInfoVo videoOneInfo;
	/**
	 * 视频信息2
	 */
	@Schema(description = "视频信息2")
	private AnchorVideoInfoVo videoTwoInfo;
	/**
	 * 文件信息1
	 */
	@Schema(description = "文件信息1")
	private UploadFileInfoVo fileOneInfo;
	/**
	 * 文件信息2
	 */
	@Schema(description = "文件信息2")
	private UploadFileInfoVo fileTwoInfo;
	/**
	 * 主播信息1
	 */
	@Schema(description = "主播信息1")
	private AnchorUrlInfoVo anchorOneInfo;
	/**
	 * 主播信息2
	 */
	@Schema(description = "主播信息2")
	private AnchorUrlInfoVo anchorTwoInfo;
	/**
	 * 用户昵称
	 */
	@Schema(description = "用户昵称")
	private String userNickName;

	/**
	 * 是否设置了星标 0：否 1：是
	 */
	@Schema(description = "是否设置了星标 0：否 1：是")
	private Integer hasStar;

	/**
	 * 星标信息
	 */
	@Schema(description = "星标信息")
	private SourceStarVo sourceStarInfo;

}
