package com.jiuyu.replay.third.api;

import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.ObjectUtil;
import com.jiuyu.replay.common.bll.SystemKvBll;
import com.jiuyu.replay.common.utils.RRException;
import com.jiuyu.replay.common.vo.SystemKvInfoVo;
import com.jiuyu.replay.generic.feign.system.DictDataFeign;
import com.jiuyu.replay.generic.feign.third.AiModelFeign;
import com.jiuyu.replay.generic.vo.ai.AiModelInfoVo;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.generic.vo.system.DictDataListVo;
import com.jiuyu.replay.third.bll.AiModelBll;
import com.jiuyu.replay.third.constant.VolcengineProperties;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * @author ：lujie
 * @description：
 * @date ：2025/6/3 上午11:28
 */
@Component
@AllArgsConstructor
public class AiModelApi implements AiModelFeign {

    private final AiModelBll aiModelBll;
    private final VolcengineProperties volcengineProperties;
    private DictDataFeign dictDataFeign;
    private SystemKvBll systemKvBll;

    @Override
    public AiModelInfoVo getByCode(String code) {
        R<AiModelInfoVo> byCode = aiModelBll.getByCode(code);
        if (byCode.getData() != null){
            return byCode.getData();
        }
        return null;
    }

    @Override
    public AiModelInfoVo info(Long id) {
        R<AiModelInfoVo> info = aiModelBll.info(id);
         if (info.getData() != null){
              return info.getData();
         }
        return null;
    }

    @Override
    public List<AiModelInfoVo> listDiagnosisModel() {
        List<DictDataListVo> dictDataListVo = dictDataFeign.dictDataListByCode("diagnosis_ai_model_list");
        if (ObjectUtil.isEmpty(dictDataListVo)) {
            return new ArrayList<>();
        }
        List<String> codes = dictDataListVo.stream().map(DictDataListVo::getValue).distinct().toList();
        if (ObjectUtil.isEmpty(codes)) {
            return new ArrayList<>();
        }
        R<List<AiModelInfoVo>> listR = aiModelBll.listByCodes(codes);
        RRException.create(listR);
        return listR.getData();
    }

    @Override
    public int getAiModel(String code) {
        String modelCode = code;
        if (code == null) {
            R<SystemKvInfoVo> byKey = systemKvBll.getByKey("client_ai_model_default");
            if (byKey.getData() != null) {
                modelCode = byKey.getData().getKvValue();
            }
        }
        DictDataListVo dictDataListVo = dictDataFeign.dictDataByLabel("client_ai_model", modelCode);
        if (ObjectUtil.isNotEmpty(dictDataListVo) && ObjectUtil.isNotEmpty(dictDataListVo.getValue())) {
            return NumberUtil.parseInt(dictDataListVo.getValue(), 0);
        }
        return 0;
    }

    @Override
    public AiModelInfoVo getAiModelByAiModel(Integer aiModel) {
        return aiModelBll.getAiModelByAiModel(aiModel, null);
    }
}
