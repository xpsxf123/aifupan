package com.jiuyu.replay.generic.feign.third;

import com.jiuyu.replay.generic.vo.ai.AiModelInfoVo;

import java.util.List;

/**
 * @author ：lujie
 * @description：
 * @date ：2025/6/3 上午11:27
 */
public interface AiModelFeign {

    /**
     * 根据code查询ai模型
     * @param code
     * @return
     */
    AiModelInfoVo getByCode(String code);

    /**
     * 根据id查询ai模型
     * @param id
     * @return
     */
    AiModelInfoVo info(Long id);

    /**
     * 获取ai模型列表-客户端的问答
     * @return
     */
    List<AiModelInfoVo> listDiagnosisModel();

    /**
     * 获取aiModel的值
     *
     * @param code
     * @return
     */
    int getAiModel(String code);

    /**
     * 根据aiModel获取aiModelInfo
     *
     * @param aiModel 字典：client_ai_model的value
     * @return
     */
    AiModelInfoVo getAiModelByAiModel(Integer aiModel);
}
