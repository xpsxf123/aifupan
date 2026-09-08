package com.jiuyu.replay.words.bll;

import com.jiuyu.replay.common.constant.WordsEnum;
import com.jiuyu.replay.generic.dto.power.UserDto;
import com.jiuyu.replay.generic.feign.power.UserFeign;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.generic.vo.common.code.StatusCode;
import com.jiuyu.replay.words.bo.BasicSettingsBo;
import com.jiuyu.replay.words.bo.UploadFileAnalysisBo;
import com.jiuyu.replay.words.bo.UploadFileAnalysisListBo;
import com.jiuyu.replay.words.bo.UploadFileBo;
import com.jiuyu.replay.words.bo.file.*;
import com.jiuyu.replay.words.constant.Constant;
import com.jiuyu.replay.words.producer.*;
import com.jiuyu.replay.words.rse.VideoSliceRse;
import com.jiuyu.replay.words.vo.UploadFileAnalysisRecordInfoVo;
import com.jiuyu.replay.words.vo.UploadFileAnalysisVo;
import com.jiuyu.replay.words.vo.UploadFileVO;
import com.jiuyu.replay.words.vo.file.UploadFileInfoVo;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author tisheng
 * @date 2024/9/5
 * @apinNote
 */
@Component
public class UploadFileBll {

    @Resource
    private UploadFileProducer uploadFileProducer;

    @Resource
    private UploadFileAnalysisProducer uploadFileAnalysisProducer;
    @Resource
    private UploadFileAnalysisRecordProducer uploadFileAnalysisRecordProducer;
    @Resource
    private ClientAiFavProducer clientAiFavProducer;
    @Resource
    private UploadFileDetailProducer uploadFileDetailProducer;
    @Resource
    private UserFeign userFeign;
    @Resource
    private VideoSliceRse videoSliceRse;
    @Autowired
    private BasicSettingsProducer basicSettingsProducer;


    /**
     * 复盘上传文件分析记录
     * @param uploadFileBo
     * @return
     */
    public R<PageUtils<UploadFileVO>> queryPage(UploadFileBo uploadFileBo) {
        return uploadFileProducer.queryPage(uploadFileBo);
    }

    /**
     * 保存复盘上传文件信息
     * @param uploadFileBo
     * @return
     */
    public R<String> saveUploadFile(UploadFileBo uploadFileBo) {
        return uploadFileProducer.saveUploadFile(uploadFileBo);
    }

    /**
     *保存分析内容
     * @param uploadFileAnalysisVo
     * @return
     */
    public R<String> saveuploadFileAnalysis(List<UploadFileAnalysisVo> uploadFileAnalysisVo) {
        return uploadFileAnalysisProducer.saveuploadFileAnalysis(uploadFileAnalysisVo);
    }

    /**
     *根据文件Id查询分析内容
     * @param fileId
     * @return
     */
    public R<List<UploadFileAnalysisVo>> seletByFileId(String fileId) {
        return uploadFileAnalysisProducer.seletByFileId(fileId);
    }

    /**
     * 客户端获取复盘文件上传列表
     * @param id
     * @return
     */
    public R<List<UploadFileVO>> seletList(Long id) {
       return uploadFileProducer.seletList(id);
    }

    /**
     * 客户端获取复盘文件分析内容
     * @param uploadFileAnalysisListBo
     * @return
     */
    public R<List<UploadFileAnalysisVo>> seletContent(UploadFileAnalysisListBo uploadFileAnalysisListBo) {
        return uploadFileAnalysisProducer.seletContent(uploadFileAnalysisListBo);
    }

    /**
     * 保存复盘上传文件分析记录及内容
     * @param uploadFileRecodBo
     * @return
     */
    public R<String> saveFileAnalysis(List<UploadFileAnalysisBo> uploadFileRecodBo) {
        return uploadFileAnalysisProducer.saveFileAnalysis(uploadFileRecodBo);
    }

    /**
     * 修改复盘上传文件信息
     */
    public R<String> updateUploadFile(UploadFileBo uploadFileBo) {

        return uploadFileProducer.updateUploadFile(uploadFileBo);
    }

    /**
     * 根据fileId删除
     * @param fileId
     */
    public void deleltByFileId(String fileId) {
        uploadFileProducer.deleltByFileId(fileId);
    }

    /**
     * 根据客户端上传的List<FileId>删除
     * @param fileId
     * @return
     */
    public R<String> removeByFileId(List<String> fileId) {
        return uploadFileProducer.removeByFileId(fileId);
    }

    /**
     * 清除文件的分析数据
     * @param fileId 文件唯一标识
     * @return
     */
    public void clearAnalysis(String fileId) {

        this.uploadFileAnalysisProducer.clearAnalysis(fileId);
    }

    /**
     * 根据user_id查询文件上传分析
     * @param uploadFileBo
     * @return
     */
    public R<PageUtils<UploadFileVO>> fileAnalysisByUserId(UploadFileBo uploadFileBo) {

        R<PageUtils<UploadFileVO>> pageUtilsR = uploadFileProducer.fileAnalysisByUserId(uploadFileBo);
        if(pageUtilsR.getCode() != StatusCode.SUCCESS.getCode() || pageUtilsR.getData() == null) {
            return pageUtilsR;
        }

        List<UploadFileVO> fileList = pageUtilsR.getData().getList();
        if(fileList == null || fileList.isEmpty()) {
            return pageUtilsR;
        }

        Set<Long> userIds = fileList.stream().map(UploadFileVO::getUserId).collect(Collectors.toSet());
        List<UserDto> userDtos = this.userFeign.listByIds(userIds);
        if(userDtos == null || userDtos.isEmpty()) {
            return pageUtilsR;
        }

        Map<Long, UserDto> userDtoMap = userDtos.stream().collect(Collectors.toMap(UserDto::getId, Function.identity()));
        for (UploadFileVO uploadFileVo : fileList) {
            uploadFileVo.setUserName("");
            UserDto userDto = userDtoMap.get(uploadFileVo.getUserId());
            if(userDto != null) {
                uploadFileVo.setUserName(userDto.getNickName());
            }
        }

        return pageUtilsR;

    }


    /**
     * 获取文件的第一批段落列表
     * @param fileId 文件唯一标识
     * @return
     */
    public R<List<UploadFileAnalysisVo>> listAnalysisOneByFileId(String fileId) {

        return R.ok(this.uploadFileAnalysisProducer.listAnalysisOneByFileId(fileId));
    }

    /**
     * 根据文件唯一标识获取文件信息
     * @param fileId 文件唯一标识
     * @return
     */
    public R<UploadFileInfoVo> infoByFileId(String fileId) {

        UploadFileInfoVo fileInfoVo = this.uploadFileProducer.getByFileId(fileId);

        return R.ok(fileInfoVo);
    }


    /**
     * 客户端获取文件列表
     * @param clientFileListBo 查询参数
     * @return
     */
    public R<PageUtils<UploadFileInfoVo>> clientFileList(ClientFileListBo clientFileListBo) {

        PageUtils<UploadFileInfoVo> pageUtils = uploadFileProducer.clientFileList(clientFileListBo);

        List<UploadFileInfoVo> list = pageUtils.getList();
        if(list != null && list.size() > 0) {
            // 获取分析记录
            List<String> fileIds = list.stream().map(UploadFileInfoVo::getFileId).toList();
            List<UploadFileAnalysisRecordInfoVo> uploadFileAnalysisRecordInfoVos = this.uploadFileAnalysisRecordProducer.listByFileIds(fileIds);
            if(uploadFileAnalysisRecordInfoVos != null && uploadFileAnalysisRecordInfoVos.size() > 0) {
                uploadFileAnalysisRecordInfoVos.sort(Comparator.comparing(UploadFileAnalysisRecordInfoVo::getVersion).reversed());
            }

            for (UploadFileInfoVo uploadFileInfoVo : list) {
                if(uploadFileAnalysisRecordInfoVos != null && uploadFileAnalysisRecordInfoVos.size() > 0) {
                    for (UploadFileAnalysisRecordInfoVo uploadFileAnalysisRecordInfoVo : uploadFileAnalysisRecordInfoVos) {
                        if(uploadFileInfoVo.getFileId().equals(uploadFileAnalysisRecordInfoVo.getFileId())) {
                            UploadFileAnalysisRecordInfoVo recordInfoVo = new UploadFileAnalysisRecordInfoVo();
                            recordInfoVo.setCruxWordNum(uploadFileAnalysisRecordInfoVo.getCruxWordNum());
                            recordInfoVo.setSensitiveWordNum(uploadFileAnalysisRecordInfoVo.getSensitiveWordNum());
                            recordInfoVo.setContentNum(uploadFileAnalysisRecordInfoVo.getContentNum());
                            uploadFileInfoVo.setRecordInfo(recordInfoVo);
                            break;
                        }
                    }
                }
            }

        }

        return R.ok("获取成功", pageUtils);
    }

    /**
     * 客户端删除文件
     * @param ids 文件uuid集合
     * @param tenantId 租户id
     * @param userId 用户id
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    public R<String> clientDeleteFile(List<String> ids, Long tenantId, Long userId) {

        if(ids == null || ids.size() < 1) {
            return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "输入的数据为空");
        }

        List<String> delIds = uploadFileProducer.getAllowDeleteFile(ids, tenantId, userId);
        if(delIds == null || delIds.size() < 1) {
            return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "没有权限");
        }

        uploadFileProducer.removeByFileId(delIds);

        // 删除关联的运营/违规助手信息
        this.clientAiFavProducer.removeByResourceIds(ids);

        // 删除关联的切片信息
        this.videoSliceRse.removeBySourceIds(ids);

        return R.ok("删除成功");
    }

    /**
     * 客户端根据文件唯一标识。获取文件信息
     * @param fileId 文件唯一标识
     * @param userId 用户id
     * @param tenantId 租户id
     * @return
     */
    public R<UploadFileInfoVo> clientGetFileByFileId(String fileId, Long userId, Long tenantId) {

        UploadFileInfoVo uploadFileInfoVo = uploadFileProducer.clientGetFileByFileId(fileId, userId, tenantId);
        if(uploadFileInfoVo != null) {
            return R.ok(uploadFileInfoVo);
        }

        return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "视频不存在");
    }

    /**
     * 保存或修改文件信息
     * @param uploadFileInfoBo 文件信息
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    public R<String> saveOrUpdateFile(UploadFileInfoBo uploadFileInfoBo) {

        this.uploadFileProducer.saveOrUpdateFile(uploadFileInfoBo);
        // 获取或创建详情
        UploadFileDetailBo detailBo = new UploadFileDetailBo();
        detailBo.setFileId(uploadFileInfoBo.getFileId());
        uploadFileDetailProducer.getAndSave(detailBo);

        // 更新基础配置
        BasicSettingsBo basicSettingsBo = new BasicSettingsBo();
        basicSettingsBo.setValues(uploadFileInfoBo);
        basicSettingsBo.setUserId(uploadFileInfoBo.getUserId());
        basicSettingsBo.setTenantId(uploadFileInfoBo.getTenantId());
        basicSettingsBo.setSourceType(WordsEnum.basicSettingsType.FILE.getCode());
        basicSettingsBo.setSourceId(uploadFileInfoBo.getFileId());
        basicSettingsProducer.updateAiPartialNew(basicSettingsBo);
        return R.ok();
    }

    /**
     * 修改文件的分析状态
     * @param updateFileAnalysisStatusBo 修改参数
     * @return
     */
    public R<String> updateFileAnalysisStatus(UpdateFileAnalysisStatusBo updateFileAnalysisStatusBo) {

        uploadFileProducer.updateFileAnalysisStatus(updateFileAnalysisStatusBo);
        return R.ok();
    }

    /**
     * 修改文件的行业
     * @param updateFileTradeBo 修改参数
     * @return
     */
    public R<String> updateFileTrade(UpdateFileTradeBo updateFileTradeBo) {

        uploadFileProducer.updateFileTrade(updateFileTradeBo);
        return R.ok();
    }

    /**
     * 客户端根据文件id集合获取文件列表
     * @param ids 视频uuid集合
     * @param userId 用户id
     * @param tenantId 租户id
     * @return
     */
    public R<List<UploadFileInfoVo>> clientListFileByFileIds(List<String> ids, Long userId, Long tenantId) {

        List<UploadFileInfoVo> fileInfoVoList = this.uploadFileProducer.clientListFileByFileIds(ids, userId, tenantId);

        return R.ok(fileInfoVoList);
    }
}
