package com.jiuyu.replay.ai.factory;

import com.jiuyu.replay.ai.model.AiModel;
import com.jiuyu.replay.ai.model.impl.DeepSeekAiModelImpl;
import com.jiuyu.replay.ai.model.impl.DoubaoAiModelImpl;
import com.jiuyu.replay.ai.model.impl.TongyiAiModelImpl;
import com.jiuyu.replay.common.utils.RRException;

/**
 * 模型构建工厂类
 */
public class ModelFactoryUtils {

    /**
     * 获取AI模型
     * @param resourceType  0:豆包，1:通义，2:DeepSeek
     * @return
     */
    public static AiModel getAiModel(Integer resourceType) {
        if (resourceType == 0){
            return new DoubaoAiModelImpl();
        } else if (resourceType == 1) {
            return new TongyiAiModelImpl();
        } else if (resourceType == 2) {
            return new DeepSeekAiModelImpl();
        }else{
            RRException.create("未知厂商类型");
        }
        return null;
    }

}
