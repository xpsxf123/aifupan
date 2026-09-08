package com.jiuyu.replay.generic.vo.ai;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;

/**
 * @author ：lujie
 * @description：
 * @date ：2025/5/22 下午3:30
 */
@Data
@Accessors(chain = true)
@Schema(description = "ai记录")
public class ConversationVo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "id")
    private String id;

    @Schema(description = "来源id")
    private String sourceId;

    @Schema(description = "来源类型 0视频，1文件，2对比分析")
    private Integer sourceType;

    @Schema(description = "用户id")
    private Long userId;

    @Schema(description = "租户id")
    private Long tenantId;

    @Schema(description = "助手类型")
    private Integer askType;

    @Schema(description = "code")
    private String code;

    @Schema(description = "一次 chat completion 接口调用的唯一标识")
    private String completionId;

    @Schema(description = "提示词id")
    private Long cueWordsId;

    @Schema(description = "提示词类型 0系统，1用户")
    private Integer cueWordsType;

    @Schema(description = "问答的code，一问一答的code的是一样的")
    private String qaCode;

    @Schema(description = "会话id")
    private String contextId;

    @Schema(description = "数据类型 Q答，A问")
    private String type;

    @Schema(description = "内容")
    private String content;

    @Schema(description = "问AI的真实问题")
    private String realContent;

    @Schema(description = "点赞状态 -1未点赞 0点赞，1踩")
    private int giveStatuc = -1;

    @Schema(description = "创建时间")
    private String createDate;

    @Schema(description = "创建时间戳")
    private Long createTime;

    @Schema(description = "生成html方式")
    private Integer htmlType;

    @Schema(description = "html生成状态 0：待生成，1：生成中，2：生成成功，3：生成失败")
    private Integer htmlStatus;

    @Schema(description = "html生成时间")
    private String htmlCreateDate;

    @Schema(description = "html保存路径")
    private String htmlSavePath;

    @Schema(description = "html生成错误原因")
    private String htmlCreateError;

    @Schema(description = "html保存域名")
    private String htmlDomainName;

    @Schema(description = "上一次对话的id")
    private String lastConversationId;

    @Schema(description = "优化文本")
    private String optimizeText;

    @Schema(description = "额外要求")
    private String extraRequire;

    @Schema(description = "提问类型：0：正常问题，1：重新提问")
    private Integer questionType;

    @Schema(description = "分析类型 0-普通分析，1-综合分析")
    private Integer analysisType;

    @Schema(description = "ai纠错状态 0：正常，1：纠错中，2：纠错完成")
    private Integer aiCorrectStatus;

    @Schema(description = "纠错来源类型 0：服务器生成，1：客户端生成")
    private String aiCorrectType;

    @Schema(description = "ai纠错错误原因")
    private String aiCorrectError;

    @Schema(description = "ai纠错创建时间")
    private Long aiCorrectCreateTime;
}
