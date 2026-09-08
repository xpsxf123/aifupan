package com.jiuyu.replay.words.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * @author tisheng
 * @date 2024/9/5
 * @apinNote
 */
@Data
@TableName("tb_upload_file_analysis")
public class UploadFileAnalysisEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    /**
     * 文件唯一标识
     */
    private String fileId;
    /**
     * 用户id
     */
     private Long userId;
    /**
     * 识别状态  0：成功 1：失败
     */
    private Integer status;
    /**
     * 分析Json数据
     */
    private String dataJson;
    /**
     * 行业Id
     */
    private String tradeId;
    /**
     * 当前是第几段，从1开始
     */
    private Integer paragraph;
    /**
     * 创建时间
     */
    private Date createDate;
    /**
     * 是否已删除
     */
    private Integer isDeleted;
    /**
     * 版本号
     */
    private Integer version;

}
