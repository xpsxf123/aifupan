package com.jiuyu.replay.words.bo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * @author tisheng
 * @date 2024/9/13
 * @apinNote
 */
@Data
@Schema(description = "客户端获取")
public class UploadFileAnalysisBo  implements Serializable {

    private static final long serialVersionUID = 1L;



    /**
     * ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    @Schema(description ="主键Id")
    private Long id;

    /**
     * 视频id
     */
    @Schema(description ="upload_file的id")
    private String fileId;

    /**
     * 用户id
     */
    // @Schema(description ="分享人id")
    // private Long userId;

    /**
     * 状态 0  1
     */
    @Schema(description ="状态 0 1")
    private Integer status;

    /**
     * 富文本内容
     */
    @Schema(description ="文本内容")
    private String dataJson;

    @Schema(description ="行业Id")
    private String tradeId;

    private Integer paragraph;

    @Schema(description ="逻辑删除")
    private Integer isDeleted;



}
