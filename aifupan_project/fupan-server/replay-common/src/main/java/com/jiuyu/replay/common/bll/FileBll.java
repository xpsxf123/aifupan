package com.jiuyu.replay.common.bll;

import com.jiuyu.replay.common.bo.FileBo;
import com.jiuyu.replay.common.bo.FileListBo;
import com.jiuyu.replay.common.bo.FileUpdateBo;
import com.jiuyu.replay.common.constant.CommonProperties;
import com.jiuyu.replay.common.producer.FileProducer;
import com.jiuyu.replay.common.vo.FileInfoVo;
import com.jiuyu.replay.common.vo.FileListVo;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.generic.vo.common.FileShowVo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.List;


/**
 * 文件
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-07-10 11:13:51
 */
@Component
public class FileBll {

    @Resource
    private FileProducer fileProducer;
    @Resource
    private CommonProperties commonProperties;


    /**
     * 文件列表
     * @param fileListBo 文件列表查询参数
     * @return
     */
    public R<PageUtils<FileListVo>> queryPage(FileListBo fileListBo) {

        return R.ok("获取成功", fileProducer.queryPage(fileListBo));
    }

    /**
    * 文件信息
    * @param id 文件id
    * @return
    */
    public R<FileInfoVo> info(Long id) {

        FileInfoVo fileInfoVo = fileProducer.info(id);
        return R.ok("获取成功", fileInfoVo);
    }

    /**
     * 新增文件
     * @param file 文件对象
     * @param fileName 文件名
     * @param suffix 文件类型
     * @param sort 排序
     * @param resourceType 来源类型
     * @return
     */
    public Long save(MultipartFile file, String fileName, String suffix, Integer sort, Integer resourceType, String cosSaveKey) {

        FileInfoVo fileInfoVo = fileProducer.save(file, fileName, suffix, sort, resourceType, cosSaveKey);

        return fileInfoVo.getId();
    }

    /**
     * 修改文件
     * @param fileBo 文件对象
     * @return
     */
    public R<String> update(FileBo fileBo) {

        fileProducer.update(fileBo);
        return R.ok("修改成功");
    }

    /**
     * 删除文件
     * @param id 文件id
     * @return
     */
    public R<String> delete(Long id) {

        fileProducer.deleteById(id);
        return R.ok("删除成功");
    }

    /**
     * 获取文件byte数组
     * @param fileName 文件名
     * @return
     */
    public byte[] getFileByte(String fileName) {
        FileInputStream inputStream = null;
        try {
            File file = new File(commonProperties.getImgFilePath() + fileName);
            inputStream = new FileInputStream(file);
            byte[] bytes = new byte[inputStream.available()];
            inputStream.read(bytes, 0, inputStream.available());
            return bytes;
        } catch (IOException e) {
            System.out.println("图片没找到:" + (commonProperties.getImgFilePath() + fileName));
            e.printStackTrace();
        }finally {
            if(inputStream != null){
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    /**
     * 更新文件列表
     * @param resourceId 来源id
     * @param resourceType 来源类型 0：用户头像图片 1：商品主图 2：商品详情图 999：未定义
     * @param remarks 描述
     * @param fileUpdateBos 文件列表
     */
    public void updateList(Long resourceId, Integer resourceType, String remarks, List<FileUpdateBo> fileUpdateBos) {

        this.fileProducer.updateList(resourceId, resourceType, remarks, fileUpdateBos);
    }

    /**
     * 更新文件列表
     * @param resourceId 来源id
     * @param resourceType 来源类型 0：用户头像图片 1：商品主图 2：商品详情图 999：未定义
     * @param remarks 描述
     * @param fileIdList 文件id列表
     */
    public void updateByFileIds(Long resourceId, Integer resourceType, String remarks, List<Long> fileIdList) {

        this.fileProducer.updateByFileIds(resourceId, resourceType, remarks, fileIdList);
    }

    /**
     * 更新文件信息
     * @param resourceId 来源id
     * @param resourceType 来源类型 0：用户头像图片 1：商品主图 2：商品详情图 999：未定义
     * @param remarks 描述
     * @param fileId 文件id
     */
    public void updateOne(Long resourceId, Integer resourceType, String remarks, Long fileId) {

        this.fileProducer.updateOne(resourceId, resourceType, remarks, fileId);
    }

    /**
     * 根绝来源id和来源类型获取文件列表
     * @param resourceId 来源id
     * @param resourceType 来源类型 0：用户头像图片 1：商品主图 2：商品详情图 999：未定义
     * @return
     */
    public List<FileShowVo> listByResourceIdAndType(Long resourceId, Integer resourceType) {

        return this.fileProducer.listByResourceIdAndType(resourceId, resourceType);
    }

    /**
     * 根绝来源id集合和来源类型获取文件列表
     * @param resourceIdList 来源id集合
     * @param resourceType 来源类型 0：用户头像图片 1：商品主图 2：商品详情图 999：未定义
     * @return
     */
    public List<FileShowVo> listByResourceIdListAndType(List<Long> resourceIdList, Integer resourceType) {

        return this.fileProducer.listByResourceIdListAndType(resourceIdList, resourceType);
    }

    /**
     * 根绝文件id集合获取文件列表
     * @param fileIds 文件id集合
     * @return
     */
    public List<FileShowVo> listByFileIds(Collection<Long> fileIds) {

        return this.fileProducer.listByFileIds(fileIds);
    }

    /**
     * 根绝文件id获取文件信息
     * @param fileId 文件id
     * @return
     */
    public FileShowVo infoByFileId(Long fileId) {

        return this.fileProducer.infoByFileId(fileId);
    }

    /**
     * 根绝来源id和来源类型获取文件
     * @param resourceId 来源id
     * @param resourceType 来源类型 0：用户头像图片 1：商品主图 2：商品详情图 999：未定义
     * @return
     */
    public FileShowVo getByResourceIdAndType(Long resourceId, Integer resourceType) {

        FileShowVo fileShowVo = this.fileProducer.getByResourceIdAndType(resourceId, resourceType);

        return fileShowVo;
    }

    /**
     * 根据来源id删除所有文件
     * @param resourceId 来源id
     */
    public void removeByResourceId(Long resourceId) {

        this.fileProducer.removeByResourceId(resourceId);
    }

    /**
     * 上次压缩文件
     * @param file
     * @return
     */
    public R<String> uploadAifuPa(MultipartFile file) throws IOException {
       return fileProducer.uploadAifuPa(file);
    }
}

