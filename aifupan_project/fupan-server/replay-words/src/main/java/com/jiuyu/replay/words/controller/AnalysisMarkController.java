package com.jiuyu.replay.words.controller;

import com.jiuyu.replay.common.constant.Constant;
import com.jiuyu.replay.common.vo.IdVo;
import com.jiuyu.replay.generic.feign.power.UserFeign;
import com.jiuyu.replay.generic.utils.ResultUtil;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.generic.vo.power.UserCacheVo;
import com.jiuyu.replay.words.bo.video.AnalysisMarkInfoBo;
import com.jiuyu.replay.words.producer.AnalysisMarkProducer;
import com.jiuyu.replay.words.vo.AnalysisMarkVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 段落标注控制器
 * 
 * @author liaoxin
 * @date 2025-06-07
 * @description 提供视频段落标注的增删改查功能，支持对视频内容进行标注管理
 */
@CrossOrigin
@RestController
@RequestMapping("replay/analysisMark")
@Tag(name = "笔记标注", description = "相关接口")
public class AnalysisMarkController {

    @Resource
    private AnalysisMarkProducer analysisMarkProducer;
    @Resource
    private UserFeign userFeign;

    /**
     * 新增标注
     * 
     * @param bo 新增数据，包含标注的相关信息
     * @return {@link R}<{@link Long}> 操作结果，成功返回标注ID
     * @description 为指定视频添加新的段落标注，会自动设置创建用户信息
     */
    @PostMapping("/add")
    @Operation(summary = "新增标注", description = "")
    public R<Long> add(@RequestBody @Validated(AnalysisMarkInfoBo.Add.class) AnalysisMarkInfoBo bo) {

        // 设置创建用户ID为当前登录用户
        UserCacheVo user = ResultUtil.getUserResult(userFeign.getLocalUser());
        bo.setCreateUserId(user.getId());

        // 索引段落标号检查
        if( bo.getParaphEndNo()<bo.getParaphStartNo()){
                return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "段落标号非法，开始标号不能大于结束标号");
        };
        if(bo.getParaphEndNo()==bo.getParaphStartNo()){
            //当段落标号项相等时，索引标号开始标号必须大于结束标号
            if(bo.getMarkEndIndex()<bo.getMarkStartIndex()){
                return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "段落索引非法，开始索引必须大于结束索引");
            }
        }
       

        return analysisMarkProducer.add(bo);
    }

    /**
     * 修改视频标注
     * 
     * @param bo 修改数据，包含需要更新的标注信息
     * @return {@link R}<{@link Long}> 操作结果，成功返回标注ID
     * @description 更新已存在的视频标注信息，会自动设置修改用户和修改时间
     */
    @PostMapping("/update")
    @Operation(summary = "修改标注", description = "修改时需要传递标注内容,只能修改标注内容")
    public R<Long> edit(@RequestBody @Validated(AnalysisMarkInfoBo.Update.class) AnalysisMarkInfoBo bo) {
        // 调用生产者服务编辑标注
        R<Long> edit = analysisMarkProducer.edit(bo);
        return edit;
    }

    /**
     * 删除标注
     * 
     * @param id 标注ID，需要删除的标注主键
     * @return {@link R}<{@link Long}> 操作结果，成功返回标注ID
     * @description 逻辑删除指定的视频标注，会自动设置删除标记和修改用户信息
     */
    @PostMapping("/delete")
    @Operation(summary = "删除标注", description = "删除标注")
    public R<Long> delete(@RequestBody @Validated IdVo id) {
        R<Long> edit = analysisMarkProducer.delete(id.getId());
        return edit;
    }

    /**
     * 查询视频标注
     * 
     * @param sourceId      视频ID，用于查询指定视频的标注
     * @param sourceType    视频类型，配合视频ID进行精确查询
     * @param startParaphNo 段落编号（开始编号），可选参数，用于范围查询
     * @param endParaphNo   段落编号（结束编号），可选参数，用于范围查询
     * @return {@link R}<{@link List}<{@link AnalysisMarkVo}>> 查询结果，包含该视频的所有标注信息列表
     * @description 根据视频ID和视频类型查询该视频下的所有标注信息，支持按段落编号范围过滤
     */
    @GetMapping("/query")
    @Operation(summary = "查询视频标注", description = "根据视频ID视频类型查询视频标注")
    public R<List<AnalysisMarkVo>> info(
            @Parameter(description = "视频ID", required = true) @RequestParam(required = true) String sourceId,
            @Parameter(description = "视频类型", required = true) @RequestParam(required = true) Integer sourceType,
            @Parameter(description = "段落编号（开始编号）", required = false) @RequestParam(required = false) Integer startParaphNo,
            @Parameter(description = "段落编号（结束编号）", required = false) @RequestParam(required = false) Integer endParaphNo) {
        return analysisMarkProducer.info(sourceId, sourceType, startParaphNo, endParaphNo);
    }
}