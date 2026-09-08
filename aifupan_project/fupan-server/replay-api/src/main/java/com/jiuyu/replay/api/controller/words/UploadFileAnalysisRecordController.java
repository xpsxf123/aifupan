package com.jiuyu.replay.api.controller.words;

import com.jiuyu.replay.api.logic.words.UploadFileAnalysisRecordLogic;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.words.bo.UploadFileAnalysisRecordBo;
import com.jiuyu.replay.words.bo.UploadFileAnalysisRecordListBo;
import com.jiuyu.replay.words.vo.UploadFileAnalysisRecordInfoVo;
import com.jiuyu.replay.words.vo.UploadFileAnalysisRecordListVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;


/**
 * 文件的分析记录
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-12-03 11:26:50
 */
@RestController
@CrossOrigin
@RequestMapping("words/updatefileanalysusrecord")
@Tag(name = "文件的分析记录")
public class UploadFileAnalysisRecordController {

    @Resource
    private UploadFileAnalysisRecordLogic uploadFileAnalysisRecordLogic;

    /**
     * 文件的分析记录列表
     * @param uploadFileAnalysisRecordListBo 文件的分析记录列表查询参数
     * @return
     */
    @PostMapping("/list")
    @Operation(summary = "文件的分析记录列表")
    public R<PageUtils<UploadFileAnalysisRecordListVo>> list(@Parameter(description = "文件的分析记录列表查询参数", required = true) @RequestBody UploadFileAnalysisRecordListBo uploadFileAnalysisRecordListBo){

        return uploadFileAnalysisRecordLogic.queryPage(uploadFileAnalysisRecordListBo);
    }


    /**
     * 文件的分析记录信息
     * @param id 文件的分析记录id
     * @return
     */
    @GetMapping("/info")
    @Operation(summary = "文件的分析记录信息")
    public R<UploadFileAnalysisRecordInfoVo> info(@Parameter(description = "文件的分析记录id", required = true) @RequestParam("id") Long id){

        return uploadFileAnalysisRecordLogic.info(id);
    }

    /**
     * 新增文件的分析记录
     * @param uploadFileAnalysisRecordBo 文件的分析记录对象
     * @return
     */
    @PostMapping("/save")
    @Operation(summary = "新增文件的分析记录")
    public R<String> save(@Parameter(description = "文件的分析记录对象", required = true) @RequestBody UploadFileAnalysisRecordBo uploadFileAnalysisRecordBo){

        return uploadFileAnalysisRecordLogic.save(uploadFileAnalysisRecordBo);
    }

    /**
     * 修改文件的分析记录
     * @param uploadFileAnalysisRecordBo 文件的分析记录对象
     * @return
     */
    @PostMapping("/update")
    @Operation(summary = "修改文件的分析记录")
    public R<String> update(@Parameter(description = "文件的分析记录对象", required = true) @RequestBody UploadFileAnalysisRecordBo uploadFileAnalysisRecordBo){

        return uploadFileAnalysisRecordLogic.update(uploadFileAnalysisRecordBo);
    }

    /**
     * 删除文件的分析记录
     * @param id 文件的分析记录id
     * @return
     */
    @GetMapping("/delete")
    @Operation(summary = "删除文件的分析记录")
    public R<String> delete(@Parameter(description = "文件的分析记录id", required = true) @RequestParam("id") Long id){

        return uploadFileAnalysisRecordLogic.delete(id);
    }

}
