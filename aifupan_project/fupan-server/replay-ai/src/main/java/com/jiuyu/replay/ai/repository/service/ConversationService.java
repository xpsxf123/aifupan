package com.jiuyu.replay.ai.repository.service;

import com.jiuyu.replay.ai.bo.UpdateCorrectStatusBo;
import com.jiuyu.replay.ai.bo.UpdateHtmlStatusBo;
import com.jiuyu.replay.ai.bo.UpdateLikesStatusBo;
import com.jiuyu.replay.ai.entity.ConversationEntity;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * @author ：lujie
 * @description：
 * @date ：2025/5/22 下午3:53
 */
public interface ConversationService {

    /**
     * 保存所有
     * @param list
     */
    List<ConversationEntity> saveAll(List<ConversationEntity> list);

    /**
     * 分页查询
     * @return
     */
    Page<ConversationEntity> page(ConversationEntity conversationEntity, int page, int limit);

    /**
     * 根据id查询
     * @param ids
     * @return
     */
    List<ConversationEntity> listByIds(List<String> ids);

    /**
     * 更新点赞状态
     * @param updateLikesStatusBo
     */
    void updateLikesStatus(UpdateLikesStatusBo updateLikesStatusBo);

    /**
     * 更新HTML生成状态
     *
     * @param updateHtmlStatusBo
     */
    void updateHtmlStatus(UpdateHtmlStatusBo updateHtmlStatusBo);

    /**
     * 更新AI纠正状态
     *
     * @param updateCorrectStatusBo
     */
    void updateCorrectStatus(UpdateCorrectStatusBo updateCorrectStatusBo);

}
