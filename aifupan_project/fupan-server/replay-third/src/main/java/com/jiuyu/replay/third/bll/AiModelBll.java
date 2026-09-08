package com.jiuyu.replay.third.bll;

import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.ObjectUtil;
import com.jiuyu.replay.common.constant.RedisCacheKey;
import com.jiuyu.replay.common.producer.SystemKvProducer;
import com.jiuyu.replay.common.redisOperate.AiRedisOperate;
import com.jiuyu.replay.common.utils.RRException;
import com.jiuyu.replay.common.vo.SystemKvInfoVo;
import com.jiuyu.replay.generic.feign.system.DictDataFeign;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.utils.ResultUtil;
import com.jiuyu.replay.generic.vo.ai.AiModelInfoVo;
import com.jiuyu.replay.generic.vo.ai.AiModelListVo;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.generic.vo.common.code.StatusCode;
import com.jiuyu.replay.generic.vo.common.exception.BusinessException;
import com.jiuyu.replay.generic.vo.system.DictDataListVo;
import com.jiuyu.replay.generic.vo.third.AiTempTokenVo;
import com.jiuyu.replay.generic.bo.ai.AiModelBo;
import com.jiuyu.replay.third.bo.AiModelListBo;
import com.jiuyu.replay.third.constant.VolcengineProperties;
import com.jiuyu.replay.third.producer.AiModelProducer;
import com.jiuyu.replay.third.zijie.ZiJieUtils;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;


/**
 * AI模型配置表
 *
 * @author DearSil
 * @email 2437495924@qq.com
 * @date 2025-02-27 17:19:05
 */
@Component
public class AiModelBll {

    private static final Logger log = LoggerFactory.getLogger(AiModelBll.class);
    @Resource
    private AiModelProducer aiModelProducer;
    @Resource
    private VolcengineProperties volcengineProperties;
    @Resource
    private AiRedisOperate aiRedisOperate;
    @Resource
    private DictDataFeign dictDataFeign;
    @Resource
    private SystemKvProducer systemKvProducer;
    @Resource
    private ZiJieUtils ziJieUtils;

    /**
     * AI模型配置表列表
     * @param aiModelListBo AI模型配置表列表查询参数
     * @return
     */
    public R<PageUtils<AiModelListVo>> queryPage(AiModelListBo aiModelListBo) {

        return R.ok("获取成功", aiModelProducer.queryPage(aiModelListBo));
    }

    /**
    * AI模型配置表信息
    * @param id AI模型配置表id
    * @return
    */
    public R<AiModelInfoVo> info(Long id) {

        AiModelInfoVo aiModelInfoVo = aiModelProducer.info(id);
        return R.ok("获取成功", aiModelInfoVo);
    }

    /**
     * 根据模型名称查询模型信息
     * @param modelName
     * @return
     */
    public R<AiModelInfoVo> getByModelName(String modelName){

        AiModelInfoVo aiModelInfoVo = aiModelProducer.getByModelName(modelName);
        return R.ok("获取成功", aiModelInfoVo);
    }

    /**
     * 根据模型编码查询模型信息
     * @param code
     * @return
     */
    public R<AiModelInfoVo> getByCode(String code){
        AiModelInfoVo aiModelInfoVo = aiModelProducer.getByCode(code);
        return R.ok("获取成功", aiModelInfoVo);
    }

    /**
     * 根据模型编码批量查询模型信息
     * @param codes
     * @return
     */
    public R<List<AiModelInfoVo>> listByCodes(List<String> codes){
        return R.ok("获取成功", aiModelProducer.listByCodes(codes));
    }

    /**
     * 新增AI模型配置表
     * @param aiModelBo AI模型配置表对象
     * @return
     */
    public R<String> save(AiModelBo aiModelBo) {

        AiModelInfoVo aiModelInfoVo = aiModelProducer.save(aiModelBo);
        return R.ok("添加成功");
    }

    /**
     * 修改AI模型配置表
     * @param aiModelBo AI模型配置表对象
     * @return
     */
    public R<String> update(AiModelBo aiModelBo) {

        aiModelProducer.update(aiModelBo);
        return R.ok("修改成功");
    }

    /**
     * 删除AI模型配置表
     * @param id AI模型配置表id
     * @return
     */
    public R<String> delete(Long id) {

        aiModelProducer.deleteById(id);
        return R.ok("删除成功");
    }


    /**
     * 获取分析模型临时token
     *
     * @param aiModel ai模型 字典：client_ai_model的value
     * @param code
     * @return 临时token
     */
    public AiTempTokenVo getAnalysisTempToken(Integer aiModel, String code) {
        if (ObjectUtil.isNull(aiModel) && ObjectUtil.isNull(code)) {
            throw new BusinessException(StatusCode.ILLEGAL_ARGUMENT_EX.getCode(), "ai模型不能为空");
        }

        AiModelInfoVo aiModelByAiModel = getAiModelByAiModel(aiModel, code);
        if (aiModelByAiModel == null) {
            log.error("未找到模型 aiModel = {}", aiModel);
            throw new BusinessException(StatusCode.ILLEGAL_ARGUMENT_EX.getCode(), "未找到模型");
        }
        return getAnalysisTempToken(aiModelByAiModel);
    }

    /**
     * 获取分析模型临时token
     *
     * @param code 模型编码
     * @return 临时token
     */
    public AiTempTokenVo getAnalysisTempToken(String code) {
        AiModelInfoVo infoVo = aiModelProducer.getByCode(code);
        if (infoVo == null) {
            log.error("未找到模型 aiModel = {}", code);
            throw new BusinessException(StatusCode.ILLEGAL_ARGUMENT_EX.getCode(), "未找到模型");
        }
        return getAnalysisTempToken(infoVo);
    }

    /**
     * 获取分析模型临时token
     *
     * @param aiModelByAiModel 模型信息
     * @return 临时token
     */
    public AiTempTokenVo getAnalysisTempToken(AiModelInfoVo aiModelByAiModel) {
        AiTempTokenVo result = new AiTempTokenVo();
        result.setModelId(aiModelByAiModel.getEndpointId());
        result.setModelCode(aiModelByAiModel.getModelCode());
        result.setModelDefinition(aiModelByAiModel.getModelName());
        result.setUseModelWay(1);
        result.setMaxSendMessageLength(aiModelByAiModel.getWordsNum());
        result.setOutWordNum(aiModelByAiModel.getOutWordNum());
        result.setResourceType(aiModelByAiModel.getResourceType());

        // 仅豆包模型支持生成火山方舟临时token，供C#客户端直接调用
        if (aiModelByAiModel.getResourceType() == 0) {
            String arkTempTokenByModel = ziJieUtils.getArkTempTokenByModel(aiModelByAiModel);
            if (ObjectUtil.isEmpty(arkTempTokenByModel)) {
                log.error("获取火山的临时token失败， modelCode = {}", aiModelByAiModel.getModelCode());
                throw new BusinessException(StatusCode.ILLEGAL_ARGUMENT_EX.getCode(), "火山的临时token失败");
            }
            result.setToken(arkTempTokenByModel);
        } else if (aiModelByAiModel.getResourceType() == 2) {
            result.setModelId(aiModelByAiModel.getId().toString());

        }
        result.setMode(volcengineProperties.getMode());
        result.setTruncationStrategyType(volcengineProperties.getTruncationStrategyType());
        int outSize = ObjectUtil.defaultIfNull(aiModelByAiModel.getOutSize(), 4);
        if (outSize < 0) {
            throw new BusinessException(StatusCode.ILLEGAL_ARGUMENT_EX.getCode(), "输出数据流大小不能小于0");
        }
        result.setLastHistoryTokens(outSize * 1024);
        result.setRollingTokens(volcengineProperties.getRollingTokens());
        result.setContextSaveTime(volcengineProperties.getContextSaveTime());
        result.setTempTokenSaveTime(aiRedisOperate.getAiModelExpire(RedisCacheKey.aiTempArkTokenCacheKey));
        return result;
    }

    /**
     * 获取ai模型方式
     *
     * @param modelCode 模型编码
     * @return 模型方式
     */
    public int getUseModelWay(String modelCode) {
        DictDataListVo dictDataListVo = dictDataFeign.dictDataByLabel("use_model_way", modelCode);
        int result = 1;
        if (dictDataListVo != null && ObjectUtil.isNotEmpty(dictDataListVo.getValue())) {
            result = NumberUtil.parseInt(dictDataListVo.getValue(), result);
        }
        return result;
    }

    /**
     * 根据模型编码获取模型信息
     *
     * @param aiModel 模型编码
     * @return 模型信息
     */
    public AiModelInfoVo getAiModelByAiModel(Integer aiModel, String code) {
        if (ObjectUtil.isEmpty(code)) {
            DictDataListVo dictDataListVo = dictDataFeign.dictDataByValue("client_ai_model", String.valueOf(aiModel));
            if (dictDataListVo != null) {
                code = dictDataListVo.getLabel();
            } else {
                SystemKvInfoVo clientAiModelDefault = systemKvProducer.getByKey("client_ai_model_default");
                if (clientAiModelDefault == null) {
                    log.error("未找到KV key = client_ai_model_default");
                    throw new BusinessException(StatusCode.ILLEGAL_ARGUMENT_EX.getCode(), "未找到模型");
                }
                code = clientAiModelDefault.getKvValue();
            }
        }
        return ResultUtil.getResult(this.getByCode(code));
    }

    public AiModelInfoVo getAiModelByCode(String code) {
        R<AiModelInfoVo> aiModelInfoVoR = this.getByCode(code);
        DictDataListVo dictDataListVo = dictDataFeign.dictDataByLabel("client_ai_model", code);
        if (dictDataListVo != null && ObjectUtil.isNotEmpty(aiModelInfoVoR.getData())) {
            AiModelInfoVo data = aiModelInfoVoR.getData();
            data.setAiModel(NumberUtil.parseInt(dictDataListVo.getValue(), 0));
        }
        return ResultUtil.getResult(aiModelInfoVoR);
    }

    public AiModelInfoVo getDiagnosis(Long modelId) {
        AiModelInfoVo res = aiModelProducer.info(modelId);
        RRException.isNotEmpty(res, "ai模型配置获取失败");
        return res;
    }
}

