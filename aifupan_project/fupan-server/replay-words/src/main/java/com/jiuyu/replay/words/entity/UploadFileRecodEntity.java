package com.jiuyu.replay.words.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-09-14 09:15:19
 */
@Data
@TableName("tb_upload_file_recod")
public class UploadFileRecodEntity implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * 
	 */
	@TableId(value = "id", type = IdType.INPUT)
	private Long id;
	/**
	 * 文件名称
	 */
	private String fileName;
	/**
	 * 文件id
	 */
	private String fileId;
	/**
	 * 文件类型
	 */
	private Integer fileType;
	/**
	 * 时长
	 */
	private Long fileDuration;
	/**
	 * 行业id
	 */
	private Integer tradeId;
	/**
	 * 用户id
	 */
	private Long userId;
	/**
	 * 总字数
	 */
	private Long sum;
	/**
	 * 上传时间
	 */
	private Date uploadTime;
	/**
	 * 逻辑删除
	 */
	@Schema(description ="逻辑删除")
	private Integer isDeleted;


}
