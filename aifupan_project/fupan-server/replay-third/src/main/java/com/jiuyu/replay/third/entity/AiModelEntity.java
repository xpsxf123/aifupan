package com.jiuyu.replay.third.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * AI模型配置表
 *
 * @author DearSil
 * @email 2437495924@qq.com
 * @date 2025-02-27 17:19:05
 */
@Data
@TableName("tb_ai_model")
public class AiModelEntity implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * ID
	 */
	@TableId(value = "id", type = IdType.INPUT)
	private Long id;
	/**
	 * 模型编码
	 */
	private String modelCode;
	/**
	 * 来源类型 0：豆包，1：通义，2：DeepSeek
	 */
	private Integer resourceType;
	/**
	 * 使用方式 0分析内容，1数据截图的视觉理解
	 */
	private Integer useType;
	/**
	 * 模型名称
	 */
	private String modelName;
	/**
	 * 模型id
	 */
	private String endpointId;
	/**
	 * 接口apiKey
	 */
	private String apiKey;
	/**
	 * 输出数据流大小(kb)
	 */
	private Integer outSize;
	/**
	 * 输入数据流大小(kb)
	 */
	private Integer inputSize;
	/**
	 * 缓存上下文大小(kb)
	 */
	private Integer contextSize;
	/**
	 * 限制使用字数数量
	 */
	private Integer wordsNum;
	/**
	 * 推荐输出字数
	 */
	private Integer outWordNum;
	/**
	 * 排序
	 */
	private Integer sort;
	/**
	 * 描述
	 */
	private String remarks;
	/**
	 * 创建时间
	 */
	private Date createDate;
	/**
	 * 最后修改时间
	 */
	private Date updateDate;
	/**
	 * 是否已删除
	 */
	private Integer isDeleted;

    /**
     * ai算力消费倍数
     */
    private Double consumeMultiple;


}
