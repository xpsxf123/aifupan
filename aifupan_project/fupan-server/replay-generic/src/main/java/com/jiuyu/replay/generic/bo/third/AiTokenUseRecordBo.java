package com.jiuyu.replay.generic.bo.third;

import com.jiuyu.replay.generic.vo.ai.AiReturnDataVo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.BeanUtils;

import java.io.Serializable;
import java.util.Date;

/**
 * ai的token使用记录信息
 *
 * @author lj
 * @email 
 * @date 2025-03-21 18:00:33
 */
@Data
@Schema(description = "ai的token使用记录信息")
@NoArgsConstructor
@AllArgsConstructor
public class AiTokenUseRecordBo implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * id
	 */
	@Schema(description = "id")
	private Long id;
	/**
	 * 租户
	 */
	@Schema(description = "租户")
	private Long tenantId;
	/**
	 * 用户id
	 */
	@Schema(description = "用户id")
	private Long userId;
	/**
	 * 请求来源类型  0：客户端，1：运营端
	 */
	@Schema(description = "请求来源类型  0：客户端，1：运营端")
	private Integer requestSourceType;
	/**
     * 使用来源类型 0：视频，1：文件，2对比分析，3数据截图，4提取文案视频，5主播获取关键词
	 */
    @Schema(description = "使用来源类型 0：视频，1：文件，2对比分析，3数据截图，4提取文案视频，5主播获取关键词")
	private Integer useSourceType;
	/**
	 * 使用来源id
	 */
	@Schema(description = "使用来源id")
	private String useSourceId;
	/**
	 * 使用类型 0: 运营助手，1:违规助手，2:弹幕助手，3：数据截图，4：弹幕助手
	 */
	@Schema(description = "使用类型 0: 运营助手，1:违规助手，2:弹幕助手，3：数据截图，4：弹幕助手")
	private Integer assistantType;
	/**
	 * 模型名称
	 */
	@Schema(description = "模型名称")
	private String modelName;
	/**
	 * 本次请求的id
	 */
	@Schema(description = "本次请求的id")
	private String requestId;
	/**
	 * 模型生成结束原因
     通义调用：
        stop:因模型输出自然结束，或触发输入参数中的stop条件而结束时为stop
        length: 因生成长度过长而结束
        tool_calls: 因发生工具调用
     豆包调用:
        stop表示正常生成结束
        length 表示已经到了生成的最大 token 数量
        content_filter 表示模型输出命中审核提前终止
	 */
	@Schema(description = "模型生成结束原因\n" +
			"     通义调用：\n" +
			"        stop:因模型输出自然结束，或触发输入参数中的stop条件而结束时为stop\n" +
			"        length: 因生成长度过长而结束\n" +
			"        tool_calls: 因发生工具调用\n" +
			"     豆包调用:\n" +
			"        stop表示正常生成结束\n" +
			"        length 表示已经到了生成的最大 token 数量\n" +
			"        content_filter 表示模型输出命中审核提前终止")
	private String finishReason;
	/**
	 * 输入token 数量
	 */
	@Schema(description = "输入token 数量")
	private Integer promptTokens;
	/**
	 * 输出 token 数量
	 */
	@Schema(description = "输出 token 数量")
	private Integer completionTokens;
	/**
	 * 图片的 token 数量
	 */
	@Schema(description = "图片的 token 数量")
	private Integer imageTokens;
	/**
	 * 音频的 token 数量
	 */
	@Schema(description = "音频的 token 数量")
	private Integer audioTokens;
	/**
	 * 视频的 token 数量
	 */
	@Schema(description = "视频的 token 数量")
	private Integer videoTokens;
	/**
	 * 上下文缓存的tokens数
	 */
	@Schema(description = "上下文缓存的tokens数")
	private Integer cachedTokens;
	/**
	 * 输出思维链内容花费的 token 
	 */
	@Schema(description = "输出思维链内容花费的 token ")
	private Integer reasoningTokens;
	/**
	 * 本次请求消耗的总 token 数量
	 */
	@Schema(description = "本次请求消耗的总 token 数量")
	private Integer totalTokens;
	/**
	 * 更新时间
	 */
	@Schema(description = "更新时间")
	private Date updateDate;
	/**
	 * 创建时间
	 */
	@Schema(description = "创建时间")
	private Date createDate;
	/**
	 * 备注
	 */
	@Schema(description = "备注")
	private String remarks;

    public AiTokenUseRecordBo(AiReturnDataVo aiReturnDataVo) {

    }

    public static AiTokenUseRecordBo builder(AiReturnDataVo aiReturnDataVo, String modelName) {
        AiTokenUseRecordBo res = new AiTokenUseRecordBo();
        BeanUtils.copyProperties(aiReturnDataVo, res);
        res.setId(null);
        res.setRequestSourceType(1);
        res.setModelName(modelName);
        return res;
    }
}
