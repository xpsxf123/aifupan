package com.jiuyu.replay.system.repository.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.system.bo.SeoCategoryBo;
import com.jiuyu.replay.system.bo.SeoCategoryListBo;
import com.jiuyu.replay.system.entity.SeoCategoryEntity;
import com.jiuyu.replay.system.vo.SeoCategoryListVo;
import com.jiuyu.replay.system.vo.SeoCategoryOptionVo;
import com.jiuyu.replay.system.vo.SeoSaveResultVo;

import java.util.List;

/**
 * SEO 分类。
 *
 * @author claude
 * @date 2026-08-12
 */
public interface SeoCategoryService extends IService<SeoCategoryEntity> {

    /**
     * 分页查询，含关联文章数。
     *
     * @param bo 查询参数
     * @return 分页结果
     */
    PageUtils<SeoCategoryListVo> queryPage(SeoCategoryListBo bo);

    /**
     * 查全部分类，供下拉使用，按 sort 升序。
     *
     * @return 分类选项列表
     */
    List<SeoCategoryOptionVo> listAll();

    /**
     * 新增分类。
     *
     * @param bo 入参
     * @return 含实际入库 slug 的保存结果
     */
    SeoSaveResultVo saveCategory(SeoCategoryBo bo);

    /**
     * 修改分类。
     *
     * @param bo 入参
     * @return 含实际入库 slug 的保存结果
     */
    SeoSaveResultVo updateCategory(SeoCategoryBo bo);

    /**
     * 分类详情，供编辑表单回填。
     *
     * <p>复用列表 VO 而不是新建一个 InfoVo：编辑表单需要的字段与列表完全一致，
     * 且 {@code articleCount} 正好用于「改别名」二次确认里说明影响范围
     * （该分类下有 N 篇文章，其栏目页入口会一并失效）。
     *
     * @param id 分类 ID
     * @return 详情，含关联文章数
     * @throws com.jiuyu.replay.generic.vo.common.exception.BusinessException 分类不存在或已删除
     */
    SeoCategoryListVo info(Long id);

    /**
     * 按名称精确查找分类，供批量导入使用。
     *
     * <p>不做模糊匹配：导入是批量写操作，「投放优化」误配到「投放优化技巧」
     * 会把几十篇文章一次性放进错误栏目，而运营要逐篇发现并修正。宁可报错让人确认。
     *
     * @param name 分类名称，前后空白会被忽略
     * @return 分类 ID；名称为空或匹配不到时返回 null
     */
    Long findIdByName(String name);

    /**
     * 按名称查找分类，不存在则新建，供批量导入勾选「分类不存在时自动创建」时使用。
     *
     * @param name 分类名称
     * @return 分类 ID；名称为空时返回 null
     */
    Long findOrCreateByName(String name);

    /**
     * 逻辑删除分类。
     *
     * <p>分类下有文章时不允许删除——文章的分类必填，删掉会产生孤儿数据，
     * 官网面包屑与栏目页渲染会异常。
     *
     * @param id 分类 ID
     * @throws com.jiuyu.replay.generic.vo.common.exception.BusinessException
     *         分类不存在或其下仍有文章时抛出，消息含文章数量。
     *         用 BusinessException 而非 IllegalArgumentException——后者的消息在 prod 环境会被
     *         GlobalExceptionHandler 替换成「请求参数不合法」，运营看不到真实原因
     */
    void deleteCategory(Long id);
}
