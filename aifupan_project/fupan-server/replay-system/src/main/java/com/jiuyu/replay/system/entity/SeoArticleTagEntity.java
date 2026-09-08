package com.jiuyu.replay.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * SEO 文章标签关联（多对多）。
 *
 * <p>物理删除即可：关联关系本身没有保留价值，标签删除时直接清理。
 *
 * @author claude
 * @date 2026-08-12
 */
@Data
@TableName("tb_seo_article_tag")
public class SeoArticleTagEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键，雪花 ID */
    @TableId(value = "id", type = IdType.INPUT)
    private Long id;

    /** 文章 tb_seo_article.id */
    private Long articleId;

    /** 标签 tb_seo_tag.id */
    private Long tagId;

    /** 创建时间 */
    private LocalDateTime createDate;
}
