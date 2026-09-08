package com.jiuyu.replay.system.repository.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.system.bo.SeoTagBo;
import com.jiuyu.replay.system.bo.SeoTagListBo;
import com.jiuyu.replay.system.entity.SeoTagEntity;
import com.jiuyu.replay.system.vo.SeoSaveResultVo;
import com.jiuyu.replay.system.vo.SeoTagListVo;
import com.jiuyu.replay.system.vo.SeoTagOptionVo;

import java.util.List;

/**
 * SEO 标签。
 *
 * @author claude
 * @date 2026-08-12
 */
public interface SeoTagService extends IService<SeoTagEntity> {

    /**
     * 分页查询，含关联文章数。本期不含关联文章浏览量。
     *
     * @param bo 查询参数
     * @return 分页结果
     */
    PageUtils<SeoTagListVo> queryPage(SeoTagListBo bo);

    /**
     * 查启用中的标签，供文章表单下拉使用。
     *
     * @return 标签选项列表
     */
    List<SeoTagOptionVo> listEnabled();

    /**
     * 按 ID 查标签，供文章列表回填标签名与状态。
     *
     * @param tagIds 标签 ID 集合
     * @return 标签选项列表
     */
    List<SeoTagOptionVo> listByIds(List<Long> tagIds);

    /**
     * 新增标签。
     *
     * @param bo 入参
     * @return 含实际入库 slug 的保存结果
     */
    SeoSaveResultVo saveTag(SeoTagBo bo);

    /**
     * 修改标签。
     *
     * @param bo 入参
     * @return 含实际入库 slug 的保存结果
     */
    SeoSaveResultVo updateTag(SeoTagBo bo);

    /**
     * 按名称查找或创建标签，供文章表单与批量导入的「顺手创建」使用。
     *
     * @param name 标签名称
     * @return 标签 ID
     */
    /**
     * 标签详情，供编辑表单回填。
     *
     * <p>复用列表 VO：编辑表单需要的字段与列表一致，且 {@code articleCount} 正好用于
     * 「改别名」二次确认里说明影响范围，以及判断该不该弹这个确认框
     * （禁用中的标签聚合页本就不可访问，改别名没有 301 风险）。
     *
     * @param id 标签 ID
     * @return 详情，含关联文章数
     * @throws com.jiuyu.replay.generic.vo.common.exception.BusinessException 标签不存在或已删除
     */
    SeoTagListVo info(Long id);

    /**
     * 按名称精确查找标签，含已禁用的。
     *
     * <p>供批量导入区分「本次新建」与「本来就有」。必须把已禁用的也算作存在——
     * 否则复用一个已禁用标签会被报成新建，而运营会以为系统里多了个标签。
     *
     * @param name 标签名称，前后空白会被忽略
     * @return 标签 ID；名称为空或匹配不到时返回 null
     */
    Long findIdByName(String name);

    Long findOrCreateByName(String name);

    /**
     * 批量逻辑删除标签，并清理文章关联。
     *
     * <p>标签是弱关联，有关联文章也允许删除；文章本身不受影响。
     *
     * @param ids 标签 ID 集合
     */
    void deleteTags(List<Long> ids);

    /**
     * 批量变更启用状态。
     *
     * @param ids    标签 ID 集合
     * @param status 目标状态
     */
    void changeStatus(List<Long> ids, Integer status);

    /**
     * 把给定标签中「已无关联文章」的那些自动关闭。
     *
     * <p>调用时机：文章的标签被改动、或文章被删除之后——这两种操作都可能让某个标签
     * 失去最后一篇文章。传入的是<b>操作前</b>挂在该文章上的标签 ID。
     *
     * <p><b>只自动关、不自动开</b>（2026-08-18 后台反馈第 2 条拍板）。
     * 自动关是为了防止官网出现必然 404 的空聚合页入口；
     * 而「文章数够了就自动开」会反复推翻运营手动关闭的决定——
     * 运营把某个标签关掉往往有其理由（内容质量、不想对外露出），
     * 系统不应该在下一次文章变动时又替他打开，何况这个过程没有任何提示。
     *
     * <p>因此达到阈值后是否启用，由运营在标签列表里自行决定。
     *
     * @param tagIds 待检查的标签 ID，可空
     */
    void autoDisableEmptyTags(List<Long> tagIds);
}
