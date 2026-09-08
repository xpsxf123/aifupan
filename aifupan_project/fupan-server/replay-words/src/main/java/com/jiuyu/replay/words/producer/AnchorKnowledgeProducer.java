package com.jiuyu.replay.words.producer;

import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.words.bo.AnchorKnowledgeListBo;
import com.jiuyu.replay.words.bo.AnchorKnowledgeSaveBo;
import com.jiuyu.replay.words.vo.AnchorKnowledgeListVo;
import com.jiuyu.replay.words.vo.AnchorKnowledgeVo;

/**
 * 主播级别知识库 Producer
 *
 * @author jy
 * @date 2026-06-29
 */
public interface AnchorKnowledgeProducer {

    /**
     * 分页查询（按 userId + secUid 分组聚合）
     *
     * @param listBo 列表查询参数
     * @return 分页结果
     */
    PageUtils<AnchorKnowledgeListVo> queryPage(AnchorKnowledgeListBo listBo);

    /**
     * 按用户和主播查询聚合后的知识库（PlaceholderContext 调用）
     *
     * @param userId 用户ID
     * @param secUid 主播唯一标识
     * @return 聚合三种类型的 VO，无数据返回 null
     */
    AnchorKnowledgeVo getByUserAndSecUid(Long userId, String secUid);

    /**
     * 批量保存/更新三种知识库类型（upsert）
     *
     * @param bo 含三种 content 字段的请求对象
     */
    void saveOrUpdate(AnchorKnowledgeSaveBo bo);

    /**
     * 删除用户指定主播的所有知识库（软删除）
     *
     * @param userId 用户ID
     * @param secUid 主播唯一标识
     */
    void deleteBySecUid(Long userId, String secUid);
}
