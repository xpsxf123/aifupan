package com.jiuyu.replay.system.repository.service;

import com.jiuyu.replay.system.bo.SeoSlugSuggestBo;
import com.jiuyu.replay.system.vo.SeoSlugSuggestVo;

/**
 * slug 查重与建议。
 *
 * <p>直接依赖三个 Dao 而不注入其他 Service，避免与 SeoCategoryService 等形成循环依赖。
 *
 * @author claude
 * @date 2026-08-12
 */
public interface SeoSlugService {

    /**
     * 生成 slug 建议并查重，供前端表单实时提示。
     *
     * @param bo 入参
     * @return 建议值 + 是否可用 + 占用者 + 语义备选
     */
    SeoSlugSuggestVo suggest(SeoSlugSuggestBo bo);

    /**
     * 解析出最终可入库的 slug：不冲突原样返回，冲突则追加随机数。
     *
     * @param type      对象类型 article / category / tag
     * @param name      名称或标题，slug 为空时用于生成拼音
     * @param slug      运营填写的 slug，可为空
     * @param excludeId 编辑时的自身 ID，查重排除自己；新增传 null
     * @return 可入库的 slug，保证非空且合法
     */
    String resolveSlug(String type, String name, String slug, Long excludeId);

    /**
     * 判定 slug 是否已被他人占用。
     *
     * @param type      对象类型
     * @param slug      待判定 slug
     * @param excludeId 排除的自身 ID，可为 null
     * @return 已被占用返回 true
     */
    boolean isSlugTaken(String type, String slug, Long excludeId);
}
