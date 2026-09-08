package com.jiuyu.replay.api.logic.common.impl;

import cn.hutool.core.util.ObjectUtil;
import com.jiuyu.replay.api.logic.common.CommonLogic;
import com.jiuyu.replay.common.alibaba.ImgOssUtils;
import com.jiuyu.replay.common.bll.ClientLogBll;
import com.jiuyu.replay.common.bll.FileBll;
import com.jiuyu.replay.common.bll.ImageBll;
import com.jiuyu.replay.common.bo.ClientLogBo;
import com.jiuyu.replay.common.constant.CommonProperties;
import com.jiuyu.replay.common.constant.TencentCosProperties;
import com.jiuyu.replay.common.vo.FileUploadVo;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.generic.vo.common.FileShowVo;
import com.jiuyu.replay.system.constant.Constant;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@Service
@Slf4j
public class CommonLogicImpl implements CommonLogic {

    @Resource
    private CommonProperties commonProperties;
    @Resource
    private TencentCosProperties tencentCosProperties;
    @Resource
    private ImageBll imageBll;
    @Resource
    private FileBll fileBll;
    @Resource
    private ImgOssUtils imgOssUtils;
    @Resource
    private ClientLogBll clientLogBll;

    @Override
    public R<FileShowVo> uploadImg(MultipartFile file, Integer flag, Integer sort, Integer resourceType, Integer isCheckSecurity) throws IOException {

        if (file.isEmpty()) {
            return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "上传失败，请选择文件");
        }
        if(StringUtils.isEmpty(file.getOriginalFilename()) || !file.getOriginalFilename().contains(".")){
            return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "上传失败，请注意文件格式");
        }

        if(isCheckSecurity == null) {
            isCheckSecurity = 1;
        }

        // 临时文件名前缀
        String prefix = UUID.randomUUID().toString().replaceAll("-", "");
        // 临时文件名后缀
        String suffix = file.getOriginalFilename().substring(file.getOriginalFilename().lastIndexOf(".")).toLowerCase();
        if(!suffix.equals(".jpg") && !suffix.equals(".png") && !suffix.equals(".jpeg")){
            return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "上传失败，请注意文件格式");
        }
        String fileName = prefix + suffix;
        String cosSaveKey = "img/" + fileName;

        // 上传到oss
        FileUploadVo fileUploadVo = imgOssUtils.uploadToImg(cosSaveKey, file.getBytes());
        if(ObjectUtil.isNotEmpty(fileUploadVo)) {
            if(isCheckSecurity == 1) {
                // 图片检测
                R<Boolean> booleanR = imgOssUtils.imageModerationWithOptions(fileUploadVo.getKey());
                if (booleanR.getCode() == 0 && booleanR.getData()){
                    // 保存到数据库
                    Long imgId = fileBll.save(file, fileUploadVo.getKey(), suffix, sort, resourceType, fileUploadVo.getKey());
                    FileShowVo fileVo = new FileShowVo();
                    fileVo.setName(fileUploadVo.getKey());
                    fileVo.setUrl(fileUploadVo.getUrl());
                    fileVo.setId(imgId);
                    fileVo.setSort(sort);
                    return R.ok("上传图片成功", fileVo);
                }else{
                    log.info("图片内容安全检测失败，删除图片, ossKay={}", fileUploadVo.getKey());
                    imgOssUtils.deleteObject(fileUploadVo.getKey());
                    return R.error(booleanR.getCode(), booleanR.getMsg());
                }
            }else {
                // 保存到数据库
                Long imgId = fileBll.save(file, fileUploadVo.getKey(), suffix, sort, resourceType, fileUploadVo.getKey());
                FileShowVo fileVo = new FileShowVo();
                fileVo.setName(fileUploadVo.getKey());
                fileVo.setUrl(fileUploadVo.getUrl());
                fileVo.setId(imgId);
                fileVo.setSort(sort);
                return R.ok("上传图片成功", fileVo);
            }
        }

        // 上传到cos
//        boolean isSuccess = imageBll.uploadToCos(cosSaveKey, file.getBytes());
//
//        if(isSuccess) {
//            // 保存到数据库
//            Long imgId = fileBll.save(file, fileName, suffix, sort, resourceType, cosSaveKey);
//            FileShowVo fileVo = new FileShowVo();
//            fileVo.setName(fileName);
//            fileVo.setUrl(tencentCosProperties.getPublicBucket().getAccessUrl() + "/" + cosSaveKey);
//            fileVo.setId(imgId);
//            fileVo.setSort(sort);
//            return R.ok("上传图片成功", fileVo);
//        }

//        String savePath = commonProperties.getImgFilePath() + fileName;
//
//        // 存储图片
//        boolean saveImg = imageBll.saveImg(file, savePath, flag);
//
//        // 保存到数据库
//        if(saveImg) {
//            Long imgId = fileBll.save(file, fileName, suffix, sort, resourceType);
//            FileShowVo fileVo = new FileShowVo();
//            fileVo.setName(fileName);
//            fileVo.setUrl(commonProperties.getShowFileUrl() + fileName);
//            fileVo.setId(imgId);
//            fileVo.setSort(sort);
//            return R.ok("上传图片成功", fileVo);
//        }

        return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "数据异常，上传失败");
    }

    @Override
    public byte[] showImg(String fileName) {
        if (StringUtils.isEmpty(fileName)) {
            return null;
        }

        return this.fileBll.getFileByte(fileName);

    }

    @Override
    public R<String> addClientLog(ClientLogBo clientLogBo) {
        return clientLogBll.save(clientLogBo);
    }
}
