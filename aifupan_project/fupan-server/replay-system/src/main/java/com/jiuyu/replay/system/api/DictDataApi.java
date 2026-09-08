package com.jiuyu.replay.system.api;

import com.jiuyu.replay.generic.feign.system.DictDataFeign;
import com.jiuyu.replay.generic.vo.ai.AiContentCorrectionConfigVo;
import com.jiuyu.replay.generic.vo.system.DictDataListVo;
import com.jiuyu.replay.system.bll.DictDataBll;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author ：lujie
 * @description：
 * @date ：2025/6/11 上午9:50
 */
@Service
@AllArgsConstructor
public class DictDataApi implements DictDataFeign {

    private final DictDataBll dictDataBll;

    @Override
    public DictDataListVo dictDataByValue(String code, String value) {
        return dictDataBll.dictDataTreeByValue(code, value);
    }

    @Override
    public DictDataListVo dictDataByLabel(String code, String label) {
        return dictDataBll.dictDataByLabel(code, label);
    }

    @Override
    public List<DictDataListVo> dictDataListByCode(String code) {
        return dictDataBll.dictDataListByCode(code);
    }

    @Override
    public List<DictDataListVo> dictDataParentLabelByCode(String code) {
        return dictDataBll.buildTreePathLabels(dictDataBll.dictDataListByCode(code));
    }

    @Override
    public List<DictDataListVo> dictDataTreeListByCode(String code) {
        return dictDataBll.dictDataTreeListByCode(code, true);
    }

    @Override
    public AiContentCorrectionConfigVo getContentCorrectionConfig(Integer sceneType) {
        return dictDataBll.getContentCorrectionConfig(sceneType);
    }

    @Override
    public List<DictDataListVo> dictDataListByIds(List<Long> ids) {
        return dictDataBll.dictDataListByIds(ids);
    }
}
