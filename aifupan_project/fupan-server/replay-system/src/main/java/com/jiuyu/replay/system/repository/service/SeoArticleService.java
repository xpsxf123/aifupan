package com.jiuyu.replay.system.repository.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.system.bo.SeoArticleBo;
import com.jiuyu.replay.system.bo.SeoArticleListBo;
import com.jiuyu.replay.system.entity.SeoArticleEntity;
import com.jiuyu.replay.system.vo.SeoArticleInfoVo;
import com.jiuyu.replay.system.vo.SeoArticleListVo;
import com.jiuyu.replay.system.vo.SeoSaveResultVo;

import java.util.List;

/**
 * SEO 文章。
 *
 * @author claude
 * @date 2026-08-13
 */
public interface SeoArticleService extends IService<SeoArticleEntity> {

    /**
     * 分页查询。出参含分类名与标签（带状态），不含正文。
     *
     * @param bo 查询参数
     * @return 分页结果
     */
    PageUtils<SeoArticleListVo> queryPage(SeoArticleListBo bo);

    /**
     * 查询详情，含正文与已挂标签（含已禁用的）。
     *
     * @param id 文章 ID
     * @return 详情
     */
    SeoArticleInfoVo info(Long id);

    /**
     * 新增文章。
     *
     * @param bo 入参
     * @return 含实际入库 slug 与 slugAppended 的结果
     */
    SeoSaveResultVo saveArticle(SeoArticleBo bo);

    /**
     * 修改文章。
     *
     * @param bo 入参
     * @return 含实际入库 slug 与 slugAppended 的结果
     */
    SeoSaveResultVo updateArticle(SeoArticleBo bo);

    /**
     * 批量逻辑删除，并清理标签关联。
     *
     * @param ids 文章 ID 集合
     */
    void deleteArticles(List<Long> ids);

    /**
     * 变更发布状态。
     *
     * <p>首次发布时写入 publishTime；下架不清空、重新发布不覆盖——保持首次发布时间，
     * 避免搜索引擎重新判定内容时效。
     *
     * @param id     文章 ID
     * @param status 目标状态
     */
    void changeStatus(Long id, Integer status);

    /**
     * 统计各分类下未删除的文章数（含已下架）。
     *
     * @param categoryIds 分类 ID 集合
     * @return categoryId -> 文章数
     */
    java.util.Map<Long, Integer> countByCategoryIds(List<Long> categoryIds);
}
