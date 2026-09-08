package com.jiuyu.replay.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * SEO 标签（官网长尾聚合页）。
 *
 * @author claude
 * @date 2026-08-12
 */
@Data
@TableName("tb_seo_tag")
public class SeoTagEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键，雪花 ID */
    @TableId(value = "id", type = IdType.INPUT)
    private Long id;

    /** 标签名称，业务限 1-20 字；删除时改写墓碑值 */
    private String tagName;

    /** 拼音别名，官网 /tag/{slug}；删除时改写墓碑值 */
    private String slug;

    /** 状态：1 启用 0 禁用。禁用后聚合页不可访问，但文章关联关系保留 */
    private Integer tagStatus;

    /** 软删除标记：0 正常 1 已删除 */
    private Integer isDeleted;

    /** 创建时间 */
    private LocalDateTime createDate;

    /** 最后修改时间 */
    private LocalDateTime updateDate;
}
