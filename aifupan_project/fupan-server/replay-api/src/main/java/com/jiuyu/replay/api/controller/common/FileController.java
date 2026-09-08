package com.jiuyu.replay.api.controller.common;

import com.jiuyu.replay.api.logic.common.FileLogic;
import com.jiuyu.replay.common.bo.FileListBo;
import com.jiuyu.replay.common.bo.UpdateFileBo;
import com.jiuyu.replay.common.vo.FileListVo;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.generic.vo.common.FileShowVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;


/**
 * 文件
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-07-15 18:18:36
 */
@RestController
@CrossOrigin
@RequestMapping("replay/file")
@Tag(name = "文件")
public class FileController {

    @Resource
    private FileLogic fileLogic;

    /**
     * 文件列表
     * @param fileListBo 文件列表查询参数
     * @return
     */
    @PostMapping("/list")
    @Operation(summary = "文件列表")
    public R<PageUtils<FileListVo>> list(@Parameter(description = "文件列表查询参数", required = true) @RequestBody FileListBo fileListBo){

        return fileLogic.queryPage(fileListBo);
    }


    /**
     * 根据条件获取单个文件
     * @param resourceId 来源id
     * @param resourceType 来源类型 0：用户头像图片 1：商品主图 2：商品详情图 999：未定义
     * @return
     */
    @GetMapping("/showOne")
    @Operation(summary = "文件信息")
    public R<FileShowVo> showOne(@Parameter(description = "来源id") @RequestParam(value = "resourceId", required = false) Long resourceId,
                                 @Parameter(description = "来源类型 0：用户头像图片 1：商品主图 2：商品详情图 999：未定义", required = true) @RequestParam("resourceType") Integer resourceType){

        return fileLogic.showOne(resourceId, resourceType);
    }


    /**
     * 修改文件
     * @param updateFileBo 文件对象
     * @return
     */
    @PostMapping("/updateFile")
    @Operation(summary = "修改文件")
    public R<String> updateFile(@Parameter(description = "文件对象", required = true) @RequestBody UpdateFileBo updateFileBo){

        return fileLogic.update(updateFileBo);
    }

    /**
     * 删除文件
     * @param id 文件id
     * @return
     */
    @GetMapping("/delete")
    @Operation(summary = "删除文件")
    public R<String> delete(@Parameter(description = "文件id", required = true) @RequestParam("id") Long id){

        return fileLogic.delete(id);
    }

    /**
     * 上传压缩文件到服务器中
     */
    @GetMapping("/uploadAifuPa")
    @Operation(summary = "上传压缩文件")
    public R<String>uploadAifuPa(@RequestParam("file") MultipartFile file) throws IOException {
        return fileLogic.uploadAifuPa(file);
    }
}
