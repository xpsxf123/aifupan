package com.jiuyu.replay.api.controller.openapi;

import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.jiuyu.replay.api.annotation.UserLock;
import com.jiuyu.replay.api.logic.system.DictDataLogic;
import com.jiuyu.replay.api.logic.third.CosThumbsFileLogic;
import com.jiuyu.replay.api.logic.third.ProxyIpLogic;
import com.jiuyu.replay.api.logic.words.AiAnalysisLogic;
import com.jiuyu.replay.api.logic.words.CueWordsLogic;
import com.jiuyu.replay.common.constant.RedisCacheKey;
import com.jiuyu.replay.common.vo.AiIdentityAndAdditionalVo;
import com.jiuyu.replay.generic.bo.words.CueWordsListBo;
import com.jiuyu.replay.generic.bo.words.cue.CueWordsPageBo;
import com.jiuyu.replay.generic.feign.words.CueWordsFeign;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.generic.vo.system.DictDataListVo;
import com.jiuyu.replay.generic.vo.system.DictDataVo;
import com.jiuyu.replay.generic.vo.third.AiTempTokenVo;
import com.jiuyu.replay.generic.vo.words.CueWordsListVo;
import com.jiuyu.replay.power.utils.GlobalObject;
import com.jiuyu.replay.power.vo.UserCacheVo;
import com.jiuyu.replay.system.bo.DictDataListBo;
import com.jiuyu.replay.third.bo.CosThumbsFileBo;
import com.jiuyu.replay.third.vo.CosThumbsFileInfoVo;
import com.jiuyu.replay.third.vo.ProxyIpRecordInfoVo;
import com.volcengine.ApiException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author ：lujie
 * @description：客户端openAPI-2.1.00
 * @date ：2025/1/14 上午11:45
 */
@RestController
@CrossOrigin
@RequestMapping("replay/openapi/v2200")
@Tag(name = "客户端openAPI-2.2.00")
@AllArgsConstructor
public class ClientOpenApi2200 {

    private final AiAnalysisLogic aiAnalysisLogic;
    private final DictDataLogic dictDataLogic;
    private final RedisTemplate redisTemplate;
    private final CosThumbsFileLogic cosThumbsFileLogic;
    private final CueWordsLogic cueWordsLogic;
    private final ProxyIpLogic proxyIpLogic;
    private final CueWordsFeign cueWordsFeign;

    /**
     * 获取代理ip
     * @param forceUpdate 是否强制更换新的IP
     * @param validityType 时效类型 0：短效 1：长效
     * @return
     */
    @Operation(summary = "获取代理ip")
    @GetMapping("/getProxyIp")
    public R<ProxyIpRecordInfoVo> getProxyIp(@Parameter(description = "是否强制更换新的IP", required = true) @RequestParam("forceUpdate") Boolean forceUpdate,
                                             @Parameter(description = "时效类型 0：短效 1：长效") @RequestParam("validityType") Integer validityType) {

        if(StringUtils.isEmpty(validityType)) {
            validityType = 0;
        }

        return proxyIpLogic.getProxyIp(forceUpdate, validityType);
    }

    @Operation(summary = "获取ai的临时token")
    @GetMapping("getArkTempToken")
    public R<AiTempTokenVo> getArkTempToken() throws ApiException {
        return aiAnalysisLogic.getArkTempToken();
    }

    @Operation(summary = "获取ai的身份设置和额外要求")
    @GetMapping("identityAndAdditionalList")
    public R<AiIdentityAndAdditionalVo> IdentityAndAdditionalList(){
        AiIdentityAndAdditionalVo result = new AiIdentityAndAdditionalVo();

        DictDataListBo dictDataListBo = new DictDataListBo();
        dictDataListBo.setLimit(-1);

        // 获取AI身份设置
        dictDataListBo.setTypeLogo("ai_identity");
        R<PageUtils<DictDataListVo>> pageUtilsR = dictDataLogic.queryPage(dictDataListBo);
        if (pageUtilsR.getCode() == 0 && ObjectUtil.isNotEmpty(pageUtilsR.getData()) && ObjectUtil.isNotEmpty(pageUtilsR.getData().getList())  && !pageUtilsR.getData().getList().isEmpty()){
            result.setIdentityList(pageUtilsR.getData().getList().stream().map(DictDataVo::getValue).toList());
        }

        // 获取AI额外要求设置
        dictDataListBo.setTypeLogo("ai_out_additional");
        pageUtilsR = dictDataLogic.queryPage(dictDataListBo);
        if (pageUtilsR.getCode() == 0 && ObjectUtil.isNotEmpty(pageUtilsR.getData()) && ObjectUtil.isNotEmpty(pageUtilsR.getData().getList())  && !pageUtilsR.getData().getList().isEmpty()){
            result.setAdditionalList(pageUtilsR.getData().getList().stream().map(DictDataVo::getValue).toList());
        }

        return R.ok(result);
    }

    @UserLock
    @Operation(summary = "获取上下文缓存id")
    @GetMapping("getContextId")
    public R<Object> getContextId(String sourceId, Integer sourceType, Integer type){
        UserCacheVo localUser = GlobalObject.getLocalUser();
        return R.ok(redisTemplate.opsForValue().get(RedisCacheKey.getRedisKey(RedisCacheKey.aiVideoContextIdCacheKey, localUser.getId(),type, StrUtil.format("{}#{}", sourceType, sourceId))));
    }

    @Operation(summary = "保存上下文缓存数据")
    @PostMapping("updateCosThumbsFile")
    @UserLock
    public R<String> saveOrUpdateCosThumbsFile(@RequestBody CosThumbsFileBo thumbsFileBo) throws Throwable {
        return cosThumbsFileLogic.saveOrUpdateCosThumbsFile(thumbsFileBo);
    }

    @Operation(summary = "根据contextId获取上下文的记录")
    @GetMapping("/getCosThumbsFileByContextId")
    public R<CosThumbsFileInfoVo> getCosThumbsFileByContextId(String contextId){
        return cosThumbsFileLogic.getCosThumbsFileByContextId(contextId);
    }


    /**
     * 获取当前对应的提示词
     * @param cueWordsListBo
     * @return
     */
    @Operation(summary = "获取当前对应的提示词")
    @PostMapping("pageCueWords")
    public R<PageUtils<CueWordsListVo>> pageCueWords(@Parameter(description = "提示词列表查询参数", required = true) @RequestBody CueWordsListBo cueWordsListBo){
        if (ObjectUtil.equals(cueWordsListBo.getScope(), 1)) {
            if (ObjectUtil.isEmpty(cueWordsListBo.getApplyTo())) cueWordsListBo.setApplyTo(0);
            return cueWordsLogic.queryPage(cueWordsListBo);
        }
        CueWordsPageBo bo = new CueWordsPageBo();
        bo.setSourceId(cueWordsListBo.getSourceId());
        bo.setSourceType(cueWordsListBo.getSourceType());
        bo.setCueType(cueWordsListBo.getCueType());
        bo.setPage(cueWordsListBo.getPage());
        bo.setLimit(cueWordsListBo.getLimit());
        return R.ok(cueWordsFeign.pageCueWords(bo));
    }

    @Operation(summary = "根据字数获取要切换模型")
    @GetMapping("/getAiModel")
    public R<Integer> getAiModel(@Parameter(description = "当前文章的数字") Integer currentNum){
        AiIdentityAndAdditionalVo result = new AiIdentityAndAdditionalVo();
        DictDataListBo dictDataListBo = new DictDataListBo();
        dictDataListBo.setLimit(-1);
        // 获取AI额外要求设置
        dictDataListBo.setTypeLogo("maxWordsNumChange");
        R<PageUtils<DictDataListVo>> pageUtilsR = dictDataLogic.queryPage(dictDataListBo);
        if (pageUtilsR.getCode() == 0 && ObjectUtil.isNotEmpty(pageUtilsR.getData()) && ObjectUtil.isNotEmpty(pageUtilsR.getData().getList())  && !pageUtilsR.getData().getList().isEmpty()){
            List<DictDataListVo> list = pageUtilsR.getData().getList();
            DictDataListVo dictDataListVo = list.stream().filter(val -> {
                if (StrUtil.isNotEmpty(val.getLabel())) {
                    String[] split = val.getLabel().split("-");
                    if (ObjectUtil.isNotEmpty(split)) {
                        if (NumberUtil.parseInt(split[0], 0) <= currentNum && NumberUtil.parseInt(split[1], 0) >= currentNum) {
                            return true;
                        }
                    }
                }
                return false;
            }).findAny().orElse(null);
            if (dictDataListVo != null){
                return R.ok(NumberUtil.parseInt(dictDataListVo.getValue(), 0));
            }
        }
        return R.ok(0);
    }

}
