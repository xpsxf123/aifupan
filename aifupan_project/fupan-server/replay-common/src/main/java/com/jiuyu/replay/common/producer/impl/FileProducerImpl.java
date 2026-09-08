package com.jiuyu.replay.common.producer.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.jiuyu.replay.common.alibaba.ImgOssUtils;
import com.jiuyu.replay.common.bo.FileBo;
import com.jiuyu.replay.common.bo.FileListBo;
import com.jiuyu.replay.common.bo.FileUpdateBo;
import com.jiuyu.replay.common.constant.CommonProperties;
import com.jiuyu.replay.common.constant.TencentCosProperties;
import com.jiuyu.replay.common.entity.FileEntity;
import com.jiuyu.replay.common.producer.FileProducer;
import com.jiuyu.replay.common.repository.service.FileService;
import com.jiuyu.replay.common.tencent.TencentCosUtils;
import com.jiuyu.replay.common.utils.Query;
import com.jiuyu.replay.common.utils.SnowflakeManager;
import com.jiuyu.replay.common.vo.FileInfoVo;
import com.jiuyu.replay.common.vo.FileListVo;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.FileShowVo;
import com.jiuyu.replay.generic.vo.common.R;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;


/**
 * 文件
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-07-10 11:13:51
 */
@Service
public class FileProducerImpl implements FileProducer {

    @Resource
    private FileService fileService;
    @Resource
    private CommonProperties commonProperties;
    @Resource
    private TencentCosProperties tencentCosProperties;
    @Resource
    private ImgOssUtils imgOssUtils;


    @Override
    public PageUtils<FileListVo> queryPage(FileListBo fileListBo) {
        QueryWrapper<FileEntity> wrapper = new QueryWrapper<>();
        if(!StringUtils.isEmpty(fileListBo.getKeyword())){
            wrapper.like("name", fileListBo.getKeyword());
        }
        wrapper.orderByAsc("sort");

        IPage<FileEntity> iPage = fileService.page(new Query<FileEntity>().getPage(fileListBo.getPage(), fileListBo.getLimit()), wrapper);

        PageUtils<FileListVo> pageUtils = new PageUtils<>(fileListBo.getPage(), fileListBo.getLimit(), iPage);

        List<FileEntity> records = iPage.getRecords();
        if(records != null && records.size() > 0) {
            List<FileListVo> vos = records.stream().map(item -> {
                FileListVo fileVo = new FileListVo();
                BeanUtils.copyProperties(item, fileVo);
                return fileVo;
            }).collect(Collectors.toList());

            pageUtils.setList(vos);
        }

        return pageUtils;
    }

    @Override
    public FileInfoVo info(Long id) {

        FileEntity fileEntity = fileService.getById(id);
        if(fileEntity != null) {
            FileInfoVo fileInfoVo = new FileInfoVo();
            BeanUtils.copyProperties(fileEntity, fileInfoVo);
            return fileInfoVo;
        }

        return null;
    }

    @Override
    public FileInfoVo save(MultipartFile file, String fileName, String suffix, Integer sort, Integer resourceType, String cosSaveKey) {

        FileEntity fileEntity = new FileEntity();
        fileEntity.setId(SnowflakeManager.nextValue());
        fileEntity.setFileName(fileName);
        fileEntity.setFileSize(file.getSize());
        fileEntity.setFileType(suffix);
        fileEntity.setCosSaveKey(cosSaveKey);
        if(!StringUtils.isEmpty(resourceType)) {
            fileEntity.setResourceType(resourceType);
        }else {
            fileEntity.setResourceType(999);
        }

        fileEntity.setCreateDate(new Date());
        fileEntity.setUpdateDate(new Date());
        if(!StringUtils.isEmpty(sort)) {
            fileEntity.setSort(sort);
        }

        fileService.save(fileEntity);

        FileInfoVo fileInfoVo = new FileInfoVo();
        BeanUtils.copyProperties(fileEntity, fileInfoVo);

        return fileInfoVo;
    }

    /**
     * 修改文件
     * @param fileBo 文件对象
     * @return
     */
    public void update(FileBo fileBo) {

        FileEntity fileEntity = new FileEntity();
        BeanUtils.copyProperties(fileBo, fileEntity);
        fileEntity.setUpdateDate(new Date());

        fileService.updateById(fileEntity);
    }

    /**
     * 删除文件
     * @param id 文件id
     * @return
     */
    public void deleteById(Long id) {

        fileService.removeById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateByFileIds(Long resourceId, Integer resourceType, String remarks, List<Long> fileIdList) {
        // 先将所有跟来源相关的文件解除关联
        this.unlinkCorrelations(resourceId, resourceType);

        // 添加新的关联
        if(fileIdList != null && fileIdList.size() > 0) {
            List<FileEntity> fileList = this.fileService.listByIds(fileIdList);

            for (FileEntity fileEntity : fileList) {
                fileEntity.setResourceId(resourceId);
                fileEntity.setResourceType(resourceType);
                fileEntity.setUpdateDate(new Date());
                fileEntity.setRemarks(remarks);
            }
            this.fileService.updateBatchById(fileList);
        }
    }

    @Override
    public void updateOne(Long resourceId, Integer resourceType, String remarks, Long fileId) {
        // 先将所有跟来源相关的文件解除关联
        if(StringUtils.isEmpty(resourceId)) {
            resourceId = 0L;
        }
        this.unlinkCorrelations(resourceId, resourceType);

        // 添加新的关联
        FileEntity fileEntity = this.fileService.getById(fileId);
        if(!StringUtils.isEmpty(resourceId)) {
            fileEntity.setResourceId(resourceId);
        }
        fileEntity.setResourceType(resourceType);
        fileEntity.setUpdateDate(new Date());
        fileEntity.setRemarks(remarks);
        this.fileService.updateById(fileEntity);

    }

    @Override
    public List<FileShowVo> listByResourceIdAndType(Long resourceId, Integer resourceType) {

        List<FileEntity> fileEntities = this.fileService.list(
                new QueryWrapper<FileEntity>().eq("resource_id", resourceId).eq("resource_type", resourceType).orderByAsc("sort"));

        return toFileVo(fileEntities);

    }

    @Override
    public List<FileShowVo> listByResourceIdListAndType(List<Long> resourceIdList, Integer resourceType) {

        List<FileEntity> fileEntities = this.fileService.list(
                new QueryWrapper<FileEntity>().in("resource_id", resourceIdList).eq("resource_type", resourceType).orderByAsc("sort"));

        return toFileVo(fileEntities);
    }

    @Override
    public FileShowVo getByResourceIdAndType(Long resourceId, Integer resourceType) {
        QueryWrapper<FileEntity> wrapper = new QueryWrapper<>();
        if(!StringUtils.isEmpty(resourceId)) {
            wrapper.eq("resource_id", resourceId);
        }
        wrapper.eq("resource_type", resourceType);
        List<FileEntity> fileEntities = this.fileService.list(wrapper);


        if(fileEntities != null && fileEntities.size() > 0) {
            FileEntity fileEntity = fileEntities.get(fileEntities.size() - 1);
            FileShowVo fileVo = new FileShowVo();
            fileVo.setId(fileEntity.getId());
            fileVo.setResourceId(fileEntity.getResourceId());
            fileVo.setName(fileEntity.getFileName());
            fileVo.setUrl(imgOssUtils.getUrl(fileEntity.getCosSaveKey()));
            return fileVo;
        }

        return null;
    }

    /**
     * 将本地磁盘的文件上传到
     * @param fileId 文件id
     * @return
     */
    private String uploadLocalFileToCos(Long fileId) {

        FileEntity fileEntity = this.fileService.getById(fileId);

        if(fileEntity != null) {
            try {
                // 读取本地文件
                Path path = Paths.get(commonProperties.getImgFilePath() + fileEntity.getFileName());
                if(Files.exists(path)) {
                    byte[] data = Files.readAllBytes(path);
                    // 上传到cos
                    ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(data);
                    String cosSaveKey = "img/" + fileEntity.getFileName();
                    Boolean isSuccess = TencentCosUtils.putStreamObject(TencentCosUtils.getPublicCosBucketName(), byteArrayInputStream, cosSaveKey);
                    if(isSuccess) {
                        // 保存到数据库
                        saveFileCosKey(fileId, cosSaveKey);
                        return cosSaveKey;
                    }
                }
            }catch (Exception e) {
                e.printStackTrace();
            }
        }

        return "";
    }

    /**
     * 保存文件的cosKey到数据库
     * @param fileId 文件id
     * @param cosSaveKey cos存储的key
     */
    private void saveFileCosKey(Long fileId, String cosSaveKey) {
        FileEntity fileEntity = new FileEntity();
        fileEntity.setId(fileId);
        fileEntity.setCosSaveKey(cosSaveKey);
        fileEntity.setUpdateDate(new Date());
        this.fileService.updateById(fileEntity);
    }

    @Override
    public void updateList(Long resourceId, Integer resourceType, String remarks, List<FileUpdateBo> fileUpdateBos) {

        // 先将所有跟来源相关的文件解除关联
        this.unlinkCorrelations(resourceId, resourceType);

        if(fileUpdateBos != null && fileUpdateBos.size() > 0) {
            List<FileEntity> fileEntities = fileUpdateBos.stream().map(item -> {
                FileEntity fileEntity = new FileEntity();
                fileEntity.setId(item.getId());
                fileEntity.setSort(item.getSort());
                fileEntity.setResourceId(resourceId);
                fileEntity.setResourceType(resourceType);
                fileEntity.setRemarks(StringUtils.isEmpty(remarks) ? null : remarks);
                fileEntity.setUpdateDate(new Date());
                return fileEntity;
            }).toList();

            this.fileService.updateBatchById(fileEntities);
        }
    }

    @Override
    public void removeByResourceId(Long resourceId) {
        this.fileService.remove(new QueryWrapper<FileEntity>().eq("resource_id", resourceId));
    }


    private List<FileShowVo> toFileVo(List<FileEntity> fileEntities) {

        if(fileEntities != null && fileEntities.size() > 0) {
            List<FileShowVo> vos = fileEntities.stream().map(item -> {
                FileShowVo fileVo = new FileShowVo();
                fileVo.setId(item.getId());
                fileVo.setResourceId(item.getResourceId());
                fileVo.setName(item.getFileName());
                fileVo.setUrl(tencentCosProperties.getPublicBucket().getAccessUrl() + "/" + item.getCosSaveKey());
                return fileVo;
            }).toList();

            return vos;
        }

        return null;
    }

    /**
     * 解除文件与来源的关联
     * @param resourceId 来源id
     * @param resourceType 来源类型
     */
    private void unlinkCorrelations(Long resourceId, Integer resourceType) {
        List<FileEntity> fileEntities = this.fileService.list(
                new QueryWrapper<FileEntity>().eq("resource_id", resourceId).eq("resource_type", resourceType));

        if(fileEntities != null && fileEntities.size() > 0) {
            for (FileEntity fileEntity : fileEntities) {
                fileEntity.setResourceId(0L);
                fileEntity.setResourceType(999);
            }
            this.fileService.updateBatchById(fileEntities);
        }
    }

    @Override
    public R<String> uploadAifuPa(MultipartFile file) throws IOException {
        try {
            //获取文件存储路径  C:\\Users\\Administrator\\Desktop\\爱复盘\
            String uploadDir="C:\\Users\\Administrator\\Desktop\\爱复盘";
            File uploadDirFile = new File(uploadDir);
            if(!uploadDirFile.exists()) {
                uploadDirFile.mkdirs();
            }
            //将文件存放到指定的位置下
            File savefile = new File(uploadDirFile, file.getOriginalFilename());
            file.transferTo(savefile);
            return R.ok();
        }catch (Exception e) {
            return R.ok(e.getMessage());
        }

    }

    @Override
    public List<FileShowVo> listByFileIds(Collection<Long> fileIds) {

        if (fileIds != null && !fileIds.isEmpty()) {
            List<FileEntity> fileEntities = this.fileService.listByIds(fileIds);
            if (fileEntities != null && !fileEntities.isEmpty()) {

                return fileIds.stream().map(fileId -> {
                    FileShowVo fileShowVo = new FileShowVo();
                    fileShowVo.setId(fileId);
                    for (FileEntity fileEntity : fileEntities) {
                        if (fileEntity.getId().equals(fileId)) {
                            fileShowVo.setResourceId(fileEntity.getResourceId());
                            fileShowVo.setName(fileEntity.getFileName());
                            fileShowVo.setUrl(imgOssUtils.getUrl(fileEntity.getCosSaveKey()));
                            break;
                        }
                    }
                    return fileShowVo;
                }).collect(Collectors.toList());

            }
        }

        return new ArrayList<>();
    }

    @Override
    public FileShowVo infoByFileId(Long fileId) {

        FileEntity fileEntity = this.fileService.getById(fileId);
        if(fileEntity != null) {
            FileShowVo fileShowVo = new FileShowVo();
            fileShowVo.setId(fileId);
            fileShowVo.setResourceId(fileEntity.getResourceId());
            fileShowVo.setName(fileEntity.getFileName());
            fileShowVo.setUrl(imgOssUtils.getUrl(fileEntity.getCosSaveKey()));

            return fileShowVo;
        }

        return null;
    }

}

