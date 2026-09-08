package com.jiuyu.replay.words.vo;

import com.jiuyu.replay.generic.vo.words.AiAnalysisSensitiveRelaInfoVo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * @author tisheng
 * @date 2024/9/5
 * @apinNote
 */
@Data
@Schema(description = "复盘上传的文件信息")
public class UploadFileVO  implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description ="文件名称")
    private String fileName;
    /**
     * 类型  0：视频 1：音频 2：文本
     */
    @Schema(description ="类型  0：视频 1：音频 2：文本")
    private Integer fileType;
    @Schema(description ="文件路径")
    private String  originalPath;
    @Schema(description ="文件新路径")
    private String  nowPath;
    @Schema(description ="文件大小")
    private Long  fileSize;
    private String fileDuration;
    @Schema(description ="错误原因")
    private String errorReason;
    @Schema(description ="行业Id")
    private Long  tradeId;
    @Schema(description ="类型")
    private String  platformType;
    @Schema(description ="分析状态 分析状态  0：未分析 1：分析中 2：分析完成 3分析错误")
    private Integer analysisStatus;
    @Schema(description ="分析时间")
    private String analysisTime;
    @Schema(description ="更新时间")
    private String uploadTime;
    @Schema(description ="上传用户")
    private Long userId;
    @Schema(description ="文件Id")
    private String fileId;
    /**
     * 文件字数，只有文本文件有
     */
    @Schema(description ="文件字数，只有文本文件有")
    private Integer fileWordNum;
    @Schema(description ="文件上传状态 0：未上传 1：已上传")
    private Integer uploadStatus;
    /**
     * 用户名称
     */
    @Schema(description ="用户名称")
    private String userName;
    /**
     * 是否已标注敏感词 0：未标注 1：已标注
     */
    @Schema(description ="是否已标注敏感词 0：未标注 1：已标注")
    private Integer isMark;
    /**
     * 占用云空间的大小，单位：M
     */
    @Schema(description ="占用云空间的大小，单位：M")
    private Integer cloudStore;
    /**
     * 租户id
     */
    @Schema(description ="租户id")
    private Long tenantId;
    /**
     * 文件切片类型 0：原文件 1：复盘切片视频 2：短视频切片视频
     */
    @Schema(description ="文件切片类型 0：原文件 1：复盘切片视频 2：短视频切片视频")
    private Integer fileSliceType;
    /**
     * 行业名称
     */
    @Schema(description ="行业名称")
    private String tradeName;
    /**
     * 分析出来的关键词总数
     */
    @Schema(description = "分析出来的关键词总数")
    private Integer sensitiveWordTotal;

    /**
     * 未匹配上词库的关键词个数
     */
    @Schema(description = "未匹配上词库的关键词个数")
    private Integer sensitiveWordMark;
    /**
     * 用户销售人员
     */
    @Schema(description = "用户销售人员")
    private String userSales;

    @Schema(description = "版本名称")
    private String packageName;

    @Schema(description = "版本过期时间")
    private Date packageExpiredTime;


    /**
     * 未在词库的词语列表
     */
    @Schema(description = "未在词库的词语列表")
    private List<AiAnalysisSensitiveRelaInfoVo> notMarkWordList;
    /**
     * 账号归属类型 0：自有账号 1：同行账号
     */
    @Schema(description = "账号归属类型 0：自有账号 1：同行账号")
    private Integer accountType;
    /**
     * 重命名
     */
    @Schema(description ="重命名")
    private String videoRename;
    /**
     * 一句话识别引擎模型，如：16k_zh
     */
    @Schema(description = "一句话识别引擎模型，如：16k_zh")
    private String engSerViceType;

}
