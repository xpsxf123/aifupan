package com.jiuyu.replay.api.controller.third;

import com.jiuyu.replay.api.logic.third.CosThumbsFileLogic;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.third.bo.CosThumbsFileBo;
import com.jiuyu.replay.third.bo.CosThumbsFileListBo;
import com.jiuyu.replay.third.vo.CosThumbsFileInfoVo;
import com.jiuyu.replay.third.vo.CosThumbsFileListVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;


/**
 * 点赞问答文件上传cos记录表
 *
 * @author DearSil
 * @email 2437495924@qq.com
 * @date 2025-02-21 16:50:31
 */
@RestController
@CrossOrigin
@RequestMapping("replay/costhumbsfile")
@Tag(name = "点赞问答文件上传cos记录表")
public class CosThumbsFileController {

    @Resource
    private CosThumbsFileLogic cosThumbsFileLogic;

    /**
     * 点赞问答文件上传cos记录表列表
     *
     * @param cosThumbsFileListBo 点赞问答文件上传cos记录表列表查询参数
     * @return
     */
    @PostMapping("/list")
    @Operation(summary = "点赞问答文件上传cos记录表列表")
    public R<PageUtils<CosThumbsFileListVo>> list(@Parameter(description = "点赞问答文件上传cos记录表列表查询参数", required = true) @RequestBody CosThumbsFileListBo cosThumbsFileListBo) {

        return cosThumbsFileLogic.queryPage(cosThumbsFileListBo);
    }


    /**
     * 点赞问答文件上传cos记录表信息
     *
     * @param id 点赞问答文件上传cos记录表id
     * @return
     */
    @GetMapping("/info")
    @Operation(summary = "点赞问答文件上传cos记录表信息")
    public R<CosThumbsFileInfoVo> info(@Parameter(description = "点赞问答文件上传cos记录表id", required = true) @RequestParam("id") Long id) {

        return cosThumbsFileLogic.info(id);
    }

    /**
     * 新增点赞问答文件上传cos记录表
     *
     * @param cosThumbsFileBo 点赞问答文件上传cos记录表对象
     * @return
     */
    @PostMapping("/save")
    @Operation(summary = "新增点赞问答文件上传cos记录表")
    public R<String> save(@Parameter(description = "点赞问答文件上传cos记录表对象", required = true) @RequestBody CosThumbsFileBo cosThumbsFileBo) {

        return cosThumbsFileLogic.save(cosThumbsFileBo);
    }

    /**
     * 修改点赞问答文件上传cos记录表
     *
     * @param cosThumbsFileBo 点赞问答文件上传cos记录表对象
     * @return
     */
    @PostMapping("/update")
    @Operation(summary = "修改点赞问答文件上传cos记录表")
    public R<String> update(@Parameter(description = "点赞问答文件上传cos记录表对象", required = true) @RequestBody CosThumbsFileBo cosThumbsFileBo) {

        return cosThumbsFileLogic.update(cosThumbsFileBo);
    }

    /**
     * 删除点赞问答文件上传cos记录表
     *
     * @param id 点赞问答文件上传cos记录表id
     * @return
     */
    @GetMapping("/delete")
    @Operation(summary = "删除点赞问答文件上传cos记录表")
    public R<String> delete(@Parameter(description = "点赞问答文件上传cos记录表id", required = true) @RequestParam("id") Long id) {

        return cosThumbsFileLogic.delete(id);
    }

}
