package com.jiuyu.replay.system.repository.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.jiuyu.replay.system.bo.SeoArticleListBo;
import com.jiuyu.replay.system.vo.SeoIdCountVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import com.jiuyu.replay.system.entity.SeoArticleEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * SEO 文章。
 *
 * @author claude
 * @date 2026-08-12
 */
@Mapper
public interface SeoArticleDao extends BaseMapper<SeoArticleEntity> {

    /**
     * 文章列表分页，SQL 见 {@code resources/mapper/SeoArticleDao.xml}。
     *
     * <p>不用 {@code LambdaQueryWrapper} 是因为要按标签筛选——那是多对多关联表上的条件，
     * Wrapper 只能靠 {@code apply()} 塞一段原生 SQL 字符串，既绕开了「查询用 LambdaQueryWrapper
     * （类型安全）」这条约束的本意，也让 SQL 以无高亮、无上下文的字面量散在 Java 里。
     *
     * @param page       分页参数
     * @param bo         查询条件（title / categoryId / articleStatus / tagId 均可空）
     * @param notDeleted 未删除标记，传 {@code SeoConstant.NOT_DELETED}
     * @return 分页结果，<b>不含 content 字段</b>
     */
    IPage<SeoArticleEntity> selectArticlePage(@Param("page") IPage<SeoArticleEntity> page,
                                              @Param("bo") SeoArticleListBo bo,
                                              @Param("notDeleted") Integer notDeleted);

    /**
     * 按分类统计文章数，SQL 见 {@code resources/mapper/SeoArticleDao.xml}。
     *
     * <p>口径：未删除、<b>含已下架</b>（openspec §6.4）。用 GROUP BY 聚合而非拉明细行计数。
     *
     * @param categoryIds 分类 ID 集合，调用方需保证非空且已分批
     * @param notDeleted  未删除标记
     * @return 每个分类的文章数，<b>只含有文章的分类</b>——0 篇的分类不在结果里
     */
    List<SeoIdCountVo> countByCategoryIds(@Param("categoryIds") List<Long> categoryIds,
                                          @Param("notDeleted") Integer notDeleted);
}
