package com.jiuyu.replay.words.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 文件的详情
 *
 * @author lyw
 * @email 1798883178@qq.com
 * @date 2025-06-03 14:14:53
 */
@Data
@TableName("tb_upload_file_detail")
public class UploadFileDetailEntity implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * ID
	 */
	@TableId(value = "id", type = IdType.INPUT)
	private Long id;
	/**
	 * 文件唯一标识
	 */
	private String fileId;
	/**
	 * 自然原文生成状态 0待生成，1生成中，2生成成功，3生成失败
	 */
	private Integer natureContentStatus;
	/**
	 * 优化原文生成状态 0待生成，1生成中，2生成成功，3生成失败
	 */
	private Integer optimizeContentStatus;
	/**
	 * 自然原文生成来源 0服务器，1客户端
	 */
	private Integer natureSourceType;
	/**
	 * 优化原文生成来源 0服务器，1客户端
	 */
	private Integer optimizeSourceType;
	/**
	 * 是否上传诊断报告 0否，1是
	 */
	private Integer hasDiagnosisReport;
	/**
	 * 用户id
	 */
	private Long userId;
	/**
	 * 租户id
	 */
	private Long tenantId;
	/**
	 * 创建时间
	 */
	private Date createDate;
	/**
	 * 更新时间
	 */
	private Date updateDate;
	/**
	 * 是否已删除
	 */
	private Integer isDeleted;

	/**
	 * 自然原文发送时间
	 */
	private Long natSetTime;

	/**
	 * 优化原文发送时间
	 */
	private Long optSetTime;

    /**
     * 0否  1扫   xxjob扫描查询自然原文
     */
    private Integer natSetJob;

    /**
     * 0否  1扫  xxjob扫描查询优化原文
     */
    private Integer optSetJob;

    /**
     * 是否已推荐行业 0未推荐，1已推荐
     */
    private Integer suggestTrade;
    /**
     * 推荐行业Id
     */
    private Long suggestTradeId;
}
