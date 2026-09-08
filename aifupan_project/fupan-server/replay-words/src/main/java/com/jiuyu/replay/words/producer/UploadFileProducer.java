package com.jiuyu.replay.words.producer;

import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.words.bo.ClientAiFavListBo;
import com.jiuyu.replay.words.bo.UploadFileAnalysisSaveBo;
import com.jiuyu.replay.words.bo.UploadFileBo;
import com.jiuyu.replay.words.bo.file.ClientFileListBo;
import com.jiuyu.replay.words.bo.file.UpdateFileAnalysisStatusBo;
import com.jiuyu.replay.words.bo.file.UpdateFileTradeBo;
import com.jiuyu.replay.words.bo.file.UploadFileInfoBo;
import com.jiuyu.replay.words.vo.UploadFileVO;
import com.jiuyu.replay.words.vo.file.UploadFileInfoVo;

import java.util.Collection;
import java.util.List;

/**
 * @author tisheng
 * @date 2024/9/5
 * @apinNote
 */
public interface UploadFileProducer {
    /**
     * 查询复盘上传文件分析记录
     * @param uploadFileBo
     * @return
     */
    R<PageUtils<UploadFileVO>>  queryPage(UploadFileBo uploadFileBo);

    /**
     * 保存复盘上传文件记录
     * @param uploadFileBo
     * @return
     */
    R<String> saveUploadFile(UploadFileBo uploadFileBo);

    /**
     * 客户端查询
     * @param id
     * @return
     */
    R<List<UploadFileVO>> seletList(Long id);

    /**
     * 修改上传文件信息
     * @param uploadFileBo
     * @return
     */
    R<String> updateUploadFile(UploadFileBo uploadFileBo);

    /**
     * 根据fileId删除
     * @param fileId
     */
    void deleltByFileId(String fileId);

    /**
     * 根据客户端上传的List<FileId>删除
     * @param fileId
     * @return
     */
    R<String> removeByFileId(List<String> fileId);

    /**
     * 批量保存分析数据
     * @param uploadFileAnalysisSaveBos
     */
    void saveBatch(List<UploadFileAnalysisSaveBo> uploadFileAnalysisSaveBos);

    /**
     * 根据唯一标识获取文件信息
     * @param fileId 文件唯一标识uuid
     * @return
     */
    UploadFileInfoVo getByFileId(String fileId);

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
    PageUtils<UploadFileInfoVo> clientFileList(ClientFileListBo clientFileListBo);

    /**
     * 条件获取ai收藏页文件数据
     * @param clientAiFavListBo 查询条件
     * @return
     */
    List<String> listByAiFav(ClientAiFavListBo clientAiFavListBo);

    /**
     * 根据文件唯一标识集合获取文件列表
     * @param fileIds 文件唯一标识集合
     * @return
     */
    List<UploadFileInfoVo> listByFileIds(Collection<String> fileIds);

    /**
     * 获取允许删除的文件id集合
     * @param ids 文件uuid集合
     * @param tenantId 租户id
     * @param userId 用户id
     * @return
     */
    List<String> getAllowDeleteFile(List<String> ids, Long tenantId, Long userId);

    /**
     * 初始化视频，将分析中的视频改成分析失败
     * @return
     */
    void initFileAnalysisStatus(Long userId, Long tenantId);

    /**
     * 客户端根据文件唯一标识。获取文件信息
     * @param fileId 文件唯一标识
     * @param userId 用户id
     * @param tenantId 租户id
     * @return
     */
    UploadFileInfoVo clientGetFileByFileId(String fileId, Long userId, Long tenantId);

    /**
     * 保存或修改文件信息
     * @param uploadFileInfoBo 文件信息
     * @return
     */
    void saveOrUpdateFile(UploadFileInfoBo uploadFileInfoBo);

    /**
     * 修改文件的分析状态
     * @param updateFileAnalysisStatusBo 修改参数
     * @return
     */
    void updateFileAnalysisStatus(UpdateFileAnalysisStatusBo updateFileAnalysisStatusBo);

    /**
     * 修改文件的行业
     * @param updateFileTradeBo 修改参数
     * @return
     */
    void updateFileTrade(UpdateFileTradeBo updateFileTradeBo);

    /**
     * 客户端根据文件id集合获取文件列表
     * @param ids 视频uuid集合
     * @param userId 用户id
     * @param tenantId 租户id
     * @return
     */
    List<UploadFileInfoVo> clientListFileByFileIds(List<String> ids, Long userId, Long tenantId);

    UploadFileInfoVo getfileByRourceId(String sourceId);
}
