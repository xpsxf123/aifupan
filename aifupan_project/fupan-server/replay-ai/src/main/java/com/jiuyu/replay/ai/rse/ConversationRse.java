package com.jiuyu.replay.ai.rse;

import com.jiuyu.replay.ai.bo.UpdateCorrectStatusBo;
import com.jiuyu.replay.ai.bo.UpdateHtmlStatusBo;
import com.jiuyu.replay.ai.bo.UpdateLikesStatusBo;
import com.jiuyu.replay.ai.bo.conversationByCueWordsIdsBo;
import com.jiuyu.replay.ai.entity.ConversationEntity;
import com.jiuyu.replay.generic.bo.ai.ConversationBo;
import com.jiuyu.replay.generic.bo.ai.ConversationListBo;
import com.jiuyu.replay.generic.vo.ai.ConversationPage;
import com.jiuyu.replay.generic.vo.ai.ConversationVo;

import java.util.List;

/**
 * @author ：lujie
 * @description：
 * @date ：2025/5/22 下午4:02
 */
public interface ConversationRse {

    /**
     * 保存数据
     *
     * @param dataList
     * @return
     */
    List<ConversationVo> saveAll(List<ConversationBo> dataList);


    /**
     * 分页查询
     * @param listBo
     * @return
     */
    ConversationPage<ConversationVo> conversationPage(ConversationListBo listBo);

    /**
     * 根据id列表查询
     * @param ids
     * @return
     */
    List<ConversationVo> listByIds(List<String> ids);

    /**
     * 更新点赞状态
     * @param updateLikesStatusBo
     */
    void updateLikesStatus(UpdateLikesStatusBo updateLikesStatusBo);

    /**
     * 查询提示词是否存在
     *
     * @param userId
     * @param tenantId
     * @param cueType
     * @param cueWordsIds
     * @return
     */
    List<Long> existsCueWords(String sourceId, Integer sourceType, Long userId, Long tenantId, Integer cueType, List<Long> cueWordsIds);

    /**
     * 根据提示词ids查询问答记录
     * @param bo
     * @return
     */
    List<ConversationVo> conversationByCueWordsIds(conversationByCueWordsIdsBo bo);

    /**
     * 查询数据是否存在
     *
     * @param conversationBo
     * @return
     */
    Boolean isExist(ConversationBo conversationBo);

    /**
     * 根据qaCode获取对应的数据
     *
     * @param qaCodes qaCode
     * @return 数据
     */
    List<ConversationEntity> listByQaCodes(List<String> qaCodes);

    /**
     * 根据id获取数据
     *
     * @param id id
     * @return 数据
     */
    ConversationVo getById(String id);

    /**
     * 更新HTML生成状态
     *
     * @param updateHtmlStatusBo 更新参数
     */
    void updateHtmlStatus(UpdateHtmlStatusBo updateHtmlStatusBo);

    /**
     * 获取HTML生成状态
     *
     * @param ids ids
     * @return HTML相关字段
     */
    List<ConversationVo> getHtmlStatus(List<String> ids);

    /**
     * 更新用户HTML生成失败
     *
     * @param userId   用户id
     * @param tenantId 租户id
     */
    void updateUserHtmlFail(Long userId, Long tenantId);

    /**
     * 批量更新HTML生成状态
     *
     * @param ids      ids
     * @param status   状态
     * @param errorMsg 错误信息
     */
    void updateHtmlStatus(List<String> ids, Integer status, String errorMsg);

    /**
     * 更新AI纠正状态
     *
     * @param updateCorrectStatusBo 更新参数
     */
    void updateCorrectStatus(UpdateCorrectStatusBo updateCorrectStatusBo);

    /**
     * 批量更新AI纠正状态
     *
     * @param ids      ids
     * @param status   状态
     * @param errorMsg 错误信息
     */
    void updateCorrectStatus(List<String> ids, Integer status, String errorMsg);

    /**
     * 更新用户AI纠正失败
     *
     * @param userId   用户id
     * @param tenantId 租户id
     */
    void updateUserCorrectFail(Long userId, Long tenantId);

    /**
     * 获取AI纠正状态
     *
     * @param ids ids
     * @return 纠正相关字段
     */
    List<ConversationVo> getCorrectStatus(List<String> ids);

    /**
     * 获取数据诊断回答内容（type=Q的最新一条）
     *
     * @param sourceId   视频id
     * @param userId     用户id
     * @param tenantId   租户id
     * @param cueWordsId 提示词id
     * @return 最新的Q类型回答内容，无则返回null
     */
    String getDataDiagnosisContent(String sourceId, Long userId, Long tenantId, Long cueWordsId);
}
