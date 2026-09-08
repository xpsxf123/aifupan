package com.jiuyu.replay.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * SEO 文章（官网内容）。
 *
 * <p>与 tb_article（客户端 H5 协议页）是两套东西，勿混用。
 *
 * @author claude
 * @date 2026-08-12
 */
@Data
@TableName("tb_seo_article")
public class SeoArticleEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键，雪花 ID */
    @TableId(value = "id", type = IdType.INPUT)
    private Long id;

    /** 文章标题，业务限 1-100 字 */
    private String title;

    /** URL 别名，官网 /article/{slug}；重复时自动追加 4 位随机数；删除时改写墓碑值 */
    private String slug;

    /** 所属分类 tb_seo_category.id，单选必填 */
    private Long categoryId;

    /** 正文 HTML，入库前已做 XSS 过滤且 h1 已降级为 h2 */
    private String content;

    /** 摘要，业务限 200 字；留空时前台取正文前 120 字 */
    private String summary;

    /** 封面图 COS 地址，必填 */
    private String coverUrl;

    /** 状态：1 已发布 0 已下架。导入落地一律为 0 */
    private Integer articleStatus;

    /** SEO 标题，建议 ≤60 字符；留空时取 title */
    private String seoTitle;

    /** SEO 描述，建议 80-160 字符 */
    private String seoDescription;

    /** SEO 关键词，英文逗号分隔，建议 ≤5 个 */
    private String seoKeywords;

    /** 浏览量：本期不做，字段预留，暂不写入不查询 */
    private Integer viewCount;

    /** 首次发布时间；下架不清空，重新发布不覆盖 */
    private LocalDateTime publishTime;

    /** 软删除标记：0 正常 1 已删除 */
    private Integer isDeleted;

    /** 创建时间 */
    private LocalDateTime createDate;

    /** 最后修改时间 */
    private LocalDateTime updateDate;
}
