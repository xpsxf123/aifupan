package com.jiuyu.replay.system.controller;

import cn.hutool.core.util.ObjectUtil;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.generic.vo.system.DictDataListVo;
import com.jiuyu.replay.system.bll.DictDataBll;
import com.jiuyu.replay.system.bll.DictTypeBll;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author ：lujie
 * @description：
 * @date ：2025/6/10 上午11:00
 */
@RestController
@CrossOrigin
@RequestMapping("replay/system/dictData")
@Tag(name = "字典类型的控制器")
@AllArgsConstructor
public class SystemDictDataController {

    public final DictDataBll dictDataBll;
    public final DictTypeBll dictTypeBll;

    @GetMapping("/listDictDataTree")
    @Operation(summary = "获取字典的树形结构")
    public R<List<DictDataListVo>> listDictDataTree(Long typeId) {
        return R.ok(dictDataBll.listDictDataTree(typeId));
    }


    @GetMapping("/uploadFileDictList")
    @Operation(summary = "获取文件上传时的平台类型字典")
    public R<List<DictDataListVo>> uploadFileDictList(){
        R<List<DictDataListVo>> replayPlatformType = this.dictDataListByCode("replay_platform_type");
        if (ObjectUtil.isNotEmpty(replayPlatformType.getData())){
            replayPlatformType.setData(replayPlatformType.getData().stream().filter(item -> !"0".equals(item.getValue())).toList());
        }
        return replayPlatformType;
    }

    @GetMapping("/dictDataByValue")
    @Operation(summary = "根据字典类型标识和字典值查询字典信息")
    public R<DictDataListVo> dictDataByValue(String code, String value){
        return R.ok(dictDataBll.dictDataByValue(code, value));
    }

    @GetMapping("/dictDataByLabel")
    @Operation(summary = "根据字典类型标识和字典标签查询字典信息")
    public R<DictDataListVo> dictDataByLabel(String code, String label) {
        return R.ok(dictDataBll.dictDataByLabel(code, label));
    }

    @GetMapping("/dictDataTreeByValue")
    @Operation(summary = "根据字典类型标识和字典值查询字典信息-label为父/子/当前")
    public R<DictDataListVo> dictDataTreeByValue(String code, String value) {
        return R.ok(dictDataBll.dictDataTreeByValue(code, value));
    }

    @GetMapping("/dictDataListByCode")
    @Operation(summary = "根据字典类型标识查询字典信息")
    public R<List<DictDataListVo>> dictDataListByCode(String code){
        return R.ok(dictDataBll.dictDataListByCode(code));
    }

    @GetMapping("/dictDataTreeListByCode")
    @Operation(summary = "根据字典类型标识查询字典信息")
    public R<List<DictDataListVo>> dictDataTreeListByCode(String code) {
        return R.ok(dictDataBll.dictDataTreeListByCode(code, false));
    }

    @GetMapping("/dictDataListByCodes")
    @Operation(summary = "根据字典类型标识查询字典信息")
    public R<Map<String, List<DictDataListVo>>> dictDataListByCodes(String codes) {
        return R.ok(dictDataBll.dictDataListByCodes(codes));
    }

    @GetMapping("/listNetworkCheckUrls")
    @Operation(summary = "获取网络延迟检测URL列表，随机返回10条")
    public R<List<DictDataListVo>> listNetworkCheckUrls() {
        List<DictDataListVo> allUrls = dictDataBll.dictDataListByCode("network_latency_check_url");
        if (ObjectUtil.isEmpty(allUrls)) {
            return R.ok(new ArrayList<>());
        }
        List<DictDataListVo> shuffled = new ArrayList<>(allUrls);
        Collections.shuffle(shuffled);
        return R.ok(shuffled.subList(0, Math.min(10, shuffled.size())));
    }

}
