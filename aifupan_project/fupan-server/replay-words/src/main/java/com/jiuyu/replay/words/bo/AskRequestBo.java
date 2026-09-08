package com.jiuyu.replay.words.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * @author ：lujie
 * @description：ai问答的请求参数
 * @date ：2025/3/22 下午4:24
 */
@Data
public class AskRequestBo {

    @Schema(description = "租户id")
    private Long tenantId;

    @Schema(description = "用户id")
    private Long userId;

    @Schema(description = "用户昵称")
    private String nikeName;

    @Schema(description = "上一次对话的id")
    private String lastConversationId;

    /**
     * 提问助手类型 0运营助手 1违规助手 2弹幕助手 3数据截图助手 4数据看板助手
     */
    @Schema(description = "提问助手类型 0运营助手 1违规助手 2弹幕助手 3数据截图助手 4数据看板助手")
    private Integer type;

    @Schema(description = "提示词id")
    private Long cueWordsId;

    @Schema(description = "提示词类型 0系统，1用户")
    private Integer cueWordsType;

    /**
     * 提问的问题
     */
    @Schema(description = "提问的问题")
    private String content;

    /**
     * 问AI的真实问题
     */
    @Schema(description = "问AI的真实问题")
    private String realContent;

    /**
     * ai的身份
     */
    @Schema(description = "ai的身份")
    private String identity;

    /**
     * 段落code(全文为0)(运营助手使用)
     */
    @Schema(description = "段落code(全文为0)(运营助手使用)")
    private String paragraphCode;

    /**
     * 段落内容(违规助手使用)
     */
    @Schema(description = "段落内容(违规助手使用)")
    private String paragraphContent;

    /**
     * 违规原因(违规助手使用)
     */
    @Schema(description = "违规原因(违规助手使用)")
    private String reasonViolation;

    /**
     * 来源的id
     */
    @Schema(description = "来源的id")
    private String sourceId;

    /**
     * 数据类型 0视频，1文件，2对比分析
     */
    @Schema(description = "数据类型 0视频，1文件，2对比分析")
    private Integer sourceType;

    /**
     * 额外要求
     */
    @Schema(description = "额外要求")
    private List<String> additionalList;

    /**
     * 文本内的参数说明
     */
    @Schema(description = "文本内的参数说明")
    private String paramsDescribe;

    /**
     * 使用的ai模型 0:doubai1.5-pro-32K， 1:deepseek-r1，2:doubao-1.5-thinking-pro，3:deepseek-chat，4:deepseek-reasoner
     */
    @Schema(description = "使用的ai模型 0:doubai1.5-pro-32K， 1:deepseek-r1，2:doubao-1.5-thinking-pro，3:deepseek-chat，4:deepseek-reasoner")
    private Integer aiModel;

    @Schema(description = "模型的使用方式 0上下文缓存对话，1对话")
    private Integer useModelWay;

    /**
     * 视频一的时间，第一个为开始时间，第二个为结束时间
     */
    @Schema(description = "视频一的时间，第一个为开始时间，第二个为结束时间")
    private List<Long> videoTimeOneList;

    /**
     * 视频二的时间，第一个为开始时间，第二个为结束时间
     */
    @Schema(description = "视频二的时间，第一个为开始时间，第二个为结束时间")
    private List<Long> videoTimeTwoList;

    @Schema(description = "是否上传数据截图(默认为1)：0不传，1传")
    private Integer uploadScreenshot;

    @Schema(description = "是否上传数据截图(默认为1)：0不传，1传")
    private Integer uploadBoard;

    @Schema(description = "其他参数")
    private Map<String, Object> otherObj;

    @Schema(description = "提示词限制在多少字")
    public int singleMaxNum = -1;

    /**
     * 上下文缓存用的 system prompt 占位符 key 列表（内存传递，不持久化）
     */
    @Schema(hidden = true)
    private List<String> systemPromptKeys;
}
