package com.jiuyu.replay.api.logic.words;

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

import java.util.List;

/**
 * @author tisheng
 * @date 2024/9/5
 * @apinNote
 */
public interface UploadFileLogic {

    /**
     * 分页查询复盘上文件
     * @param uploadFileBo
     * @return
     */
    R<PageUtils<UploadFileVO>>  queryPage(UploadFileBo uploadFileBo);

    /**
     * 保存复盘上传文件信息
     * @param uploadFileBo
     * @return
     */
    R<String>saveUploadFile(UploadFileBo uploadFileBo);

    /**
     * 保存复盘上传文件分析内容
     * @param uploadFileAnalysisVo
     * @return
     */
    R<String> saveuploadFileAnalysis(List<UploadFileAnalysisVo> uploadFileAnalysisVo);

    /**
     * 根据FileId查询分析内容
     * @param fileId
     * @return
     */
    R<List<UploadFileAnalysisVo>> seletByFileId(String fileId);

    /**
     * 客户端获取复盘上传文件列表
     * @return
     */
    R<List<UploadFileVO>> seletList();

    /**
     * 客户端获取复盘上传文件分析内容
     * @param uploadFileAnalysisListBo
     * @return
     */
    R<List<UploadFileAnalysisVo>> seletContent(UploadFileAnalysisListBo uploadFileAnalysisListBo);

    /**
     *保存复盘上传文件分析记录及内容
     * @param uploadFileRecodBo
     * @return
     */
    R<String> saveFileAnalysis(List<UploadFileAnalysisBo> uploadFileRecodBo);

    /**
     * 修改复盘上传文件
     * @param uploadFileBo
     * @return
     */
    R<String> updateUploadFile(UploadFileBo uploadFileBo);

    /**
     * 根据fileId 删除文件上传
     * @param fileId
     * @return
     */
    void deleltByFileId(String fileId);

    /**
     * 根据客户端上传的List<FileId>删除
     * @param fileId
     * @return
     */
    R<String> removeByFileId(List<String> fileId);

    /**
     * 清除文件的分析数据
     * @param fileId 文件唯一标识
     * @return
     */
    void clearAnalysis(String fileId);


    /**
     * 根据user_id查询文件上传分析
     * @param uploadFileBo
     * @return
     */
    R<PageUtils<UploadFileVO>> fileAnalysisByUserId(UploadFileBo uploadFileBo);

    /**
     * 客户端获取文件列表
     * @param clientFileListBo 查询参数
     * @return
     */
    R<PageUtils<UploadFileInfoVo>> clientFileList(ClientFileListBo clientFileListBo);

    /**
     * 客户端删除文件
     * @param ids 文件uuid集合
     * @return
     */
    R<String> clientDeleteFile(List<String> ids);

    /**
     * 客户端根据文件唯一标识。获取文件信息
     * @param fileId 文件唯一标识
     * @return
     */
    R<UploadFileInfoVo> clientGetFileByFileId(String fileId);

    /**
     * 保存或修改文件信息
     * @param uploadFileInfoBo 文件信息
     * @return
     */
    R<String> saveOrUpdateFile(UploadFileInfoBo uploadFileInfoBo);

    /**
     * 修改文件的分析状态
     * @param updateFileAnalysisStatusBo 修改参数
     * @return
     */
    R<String> updateFileAnalysisStatus(UpdateFileAnalysisStatusBo updateFileAnalysisStatusBo);

    /**
     * 修改文件的行业
     * @param updateFileTradeBo 修改参数
     * @return
     */
    R<String> updateFileTrade(UpdateFileTradeBo updateFileTradeBo);

    /**
     * 客户端根据文件id集合获取文件列表
     * @param ids 文件uuid集合
     * @return
     */
    R<List<UploadFileInfoVo>> clientListFileByFileIds(List<String> ids);
}
