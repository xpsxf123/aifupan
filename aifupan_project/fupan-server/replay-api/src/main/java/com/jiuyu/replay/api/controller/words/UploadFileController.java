package com.jiuyu.replay.api.controller.words;

import com.jiuyu.replay.api.logic.words.UploadFileLogic;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.words.bo.UploadFileAnalysisBo;
import com.jiuyu.replay.words.bo.UploadFileAnalysisListBo;
import com.jiuyu.replay.words.bo.UploadFileBo;
import com.jiuyu.replay.words.bo.file.ClientFileListBo;
import com.jiuyu.replay.words.bo.file.UpdateFileAnalysisStatusBo;
import com.jiuyu.replay.words.bo.file.UpdateFileTradeBo;
import com.jiuyu.replay.words.bo.file.UploadFileInfoBo;
import com.jiuyu.replay.words.vo.UploadFileAnalysisVo;
import com.jiuyu.replay.words.vo.UploadFileVO;
import com.jiuyu.replay.words.vo.file.UploadFileInfoVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author tisheng
 * @date 2024/9/5
 * @apinNote
 */
@RestController
@CrossOrigin
@RequestMapping("replay/UploadFile")
@Tag(name = "本地文件上传分析")
public class UploadFileController {
    @Resource
    UploadFileLogic uploadFileLogic;

    /**
     * 客户端获取文件列表
     * @param clientFileListBo 查询参数
     * @return
     */
    @PostMapping("/clientFileList")
    @Operation(summary = "客户端获取文件列表")
    public R<PageUtils<UploadFileInfoVo>> clientFileList(@RequestBody ClientFileListBo clientFileListBo) {

        return uploadFileLogic.clientFileList(clientFileListBo);
    }

    /**
     * 客户端删除文件
     * @param ids 文件uuid集合
     * @return
     */
    @PostMapping("/clientDeleteFile")
    @Operation(summary = "客户端删除文件")
    public R<String> clientDeleteFile(@Parameter(description = "文件uuid集合", required = true) @RequestBody List<String> ids) {

        return uploadFileLogic.clientDeleteFile(ids);
    }

    /**
     * 客户端根据文件唯一标识。获取文件信息
     * @param fileId 文件唯一标识
     * @return
     */
    @GetMapping("/clientGetFileByFileId")
    @Operation(summary = "客户端根据文件唯一标识。获取文件信息")
    public R<UploadFileInfoVo> clientGetFileByFileId(@Parameter(description = "文件唯一标识") @RequestParam String fileId) {

        return uploadFileLogic.clientGetFileByFileId(fileId);
    }

    /**
     * 保存或修改文件信息
     * @param uploadFileInfoBo 文件信息
     * @return
     */
    @PostMapping("/saveOrUpdateFile")
    @Operation(summary = "保存或修改文件信息")
    public R<String> saveOrUpdateFile(@RequestBody UploadFileInfoBo uploadFileInfoBo){

        return uploadFileLogic.saveOrUpdateFile(uploadFileInfoBo);
    }

    /**
     * 修改文件的分析状态
     * @param updateFileAnalysisStatusBo 修改参数
     * @return
     */
    @Operation(summary = "修改文件的分析状态")
    @PostMapping("/updateFileAnalysisStatus")
    public R<String> updateFileAnalysisStatus(@RequestBody UpdateFileAnalysisStatusBo updateFileAnalysisStatusBo) {

        return uploadFileLogic.updateFileAnalysisStatus(updateFileAnalysisStatusBo);
    }

    /**
     * 修改文件的行业
     * @param updateFileTradeBo 修改参数
     * @return
     */
    @Operation(summary = "修改文件的行业")
    @PostMapping("/updateFileTrade")
    public R<String> updateFileTrade(@RequestBody UpdateFileTradeBo updateFileTradeBo) {

        return uploadFileLogic.updateFileTrade(updateFileTradeBo);
    }

    /**
     * 客户端根据文件id集合获取文件列表
     * @param ids 文件uuid集合
     * @return
     */
    @PostMapping("/clientListFileByFileIds")
    @Operation(summary = "客户端根据文件id集合获取文件列表")
    public R<List<UploadFileInfoVo>> clientListFileByFileIds(@Parameter(description = "文件uuid集合", required = true) @RequestBody List<String> ids) {

        return uploadFileLogic.clientListFileByFileIds(ids);
    }















    /**
     * 根据user_id查询文件上传分析
     * @param uploadFileBo
     * @return
     */
    @PostMapping("/fileAnalysisByUserId")
    @Operation(summary = "根据user_id查询文件上传分析")
    public R<PageUtils<UploadFileVO>> fileAnalysisByUserId(@RequestBody UploadFileBo uploadFileBo) {
        return uploadFileLogic.fileAnalysisByUserId(uploadFileBo);
    }

    /**
     * 分页查询复盘上文件
     * @param uploadFileBo
     * @return
     */
    @PostMapping("/queryPagelist")
    @Operation(summary = "分页查询复盘上文件")
    public R<PageUtils<UploadFileVO>> queryPagelist(@Parameter(description = "分页查询复盘上文件", required = true) @RequestBody UploadFileBo uploadFileBo){
        return uploadFileLogic.queryPage(uploadFileBo);
    }

    /**
     *保存复盘上传文件信息
     * @param uploadFileBo
     * @return
     */
    @PostMapping("/saveUploadFile")
    @Operation(summary = "保存复盘上传文件信息")
    public R<String> saveUploadFile(@Parameter(description = "行业列表查询参数", required = true) @RequestBody UploadFileBo uploadFileBo){

        return uploadFileLogic.saveUploadFile(uploadFileBo);
    }

    /**
     *修改复盘上传文件信息
     * @param uploadFileBo
     * @return
     */
    @PostMapping("/updateUploadFile")
    @Operation(summary = "修改复盘上传文件信息")
    public R<String> updateUploadFile(@Parameter(description = "上传文件信息", required = true) @RequestBody UploadFileBo uploadFileBo){

        return uploadFileLogic.updateUploadFile(uploadFileBo);
    }


    /**
     * 根据FileId删除
     * @param FileId
     * @return
     */
    @GetMapping("/deleltByFileId")
    @Operation(summary = "根据FileId删除")
    public void deleltByFileId(@RequestParam String FileId){

         uploadFileLogic.deleltByFileId(FileId);
    }


    /**
     * 保存复盘上传文件分析内容
     * @param
     * @return
     */
    @PostMapping("/saveuploadFileAnalysis")
    @Operation(summary = "保存复盘上传文件分析内容")
    public R<String> saveuploadFileAnalysis(@Parameter(description = "行业列表查询参数", required = true) @RequestBody List<UploadFileAnalysisVo> uploadFileAnalysisVo) {

        return uploadFileLogic.saveuploadFileAnalysis(uploadFileAnalysisVo);
    }



    /**
     * 保存复盘上传文件分析记录及内容
     * @param
     * @return
     */
    @PostMapping("/saveFileAnalysis")
    @Operation(summary = "保存复盘上传文件分析记录及内容")
    public R<String> saveFileAnalysis(@Parameter(description = "行业列表查询参数", required = true) @RequestBody List<UploadFileAnalysisBo> list){
        return uploadFileLogic.saveFileAnalysis(list);

    }


    /**
     * 根据FileId查询分析内容
     * @param FileId
     * @return
     */
    @GetMapping("/seletByFileId")
    @Operation(summary = "根据FileId查询分析内容")
    public R<List<UploadFileAnalysisVo>> seletByFileId(@RequestParam String FileId){

        return uploadFileLogic.seletByFileId(FileId);
    }

    /**
     * 客户端获取复盘上传文件列表
     * @param
     * @return
     */
    @GetMapping("/seletList")
    @Operation(summary = "客户端获取复盘上传文件列表")
    public R<List<UploadFileVO>> seletList(){

        return uploadFileLogic.seletList();
    }

    /**
     * 客户端获取复盘上传文件分析内容
     * @param
     * @return
     */
    @PostMapping("/seletContent")
    @Operation(summary = "客户端获取复盘上传文件分析内容")
    public R<List<UploadFileAnalysisVo>> seletContent(@RequestBody UploadFileAnalysisListBo uploadFileAnalysisListBo){

        return uploadFileLogic.seletContent(uploadFileAnalysisListBo);
    }


    /**
     * 根据客户端上传的List<FileId>删除
     * @param FileId
     * @return
     */
    @PostMapping("/removeByFileId")
    @Operation(summary = "根据客户端上传的List<FileId>删除")
    public R<String> removeByFileId(@RequestBody List<String> FileId){

        return uploadFileLogic.removeByFileId(FileId);
    }

    /**
     * 清除文件的分析数据
     * @param fileId 文件唯一标识
     * @return
     */
    @GetMapping("/clearAnalysis")
    @Operation(summary = "清除文件的分析数据")
    public R<String> clearAnalysis(@RequestParam String fileId){
        this.uploadFileLogic.clearAnalysis(fileId);
        return R.ok();
    }


}
