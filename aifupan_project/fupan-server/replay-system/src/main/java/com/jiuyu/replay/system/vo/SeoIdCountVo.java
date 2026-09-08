package com.jiuyu.replay.system.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 「某个 ID 对应多少条」的聚合结果，分类与标签的 articleCount 统计共用。
 *
 * <p>专门建这个类而不是用 {@code Map<String, Object>}：MyBatis 把
 * {@code COUNT(*)} 映射成 Map 时的值类型随驱动与数据库而变（Long / BigInteger / BigDecimal 都可能），
 * 调用方只能靠 {@code toString()} 再 {@code valueOf()} 绕一圈——那既不类型安全，
 * 也让「这个 Map 里到底有哪些 key」变成必须翻 SQL 才知道的隐性契约。
 *
 * @author claude
 * @date 2026-08-13
 */
@Data
public class SeoIdCountVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 被统计的对象 ID：分类 ID 或标签 ID */
    private Long id;

    /** 该 ID 下的文章数 */
    private Integer total;
}
