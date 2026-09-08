package com.jiuyu.replay.common.producer;

import com.jiuyu.replay.common.bo.FileBo;
import com.jiuyu.replay.common.bo.FileListBo;
import com.jiuyu.replay.common.bo.FileUpdateBo;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.common.vo.FileInfoVo;
import com.jiuyu.replay.common.vo.FileListVo;
import com.jiuyu.replay.generic.vo.common.FileShowVo;
import org.springframework.web.multipart.MultipartFile;

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
public interface FileProducer {


    /**
     * 文件列表
     * @param fileListBo 文件列表查询参数
     * @return
     */
    PageUtils<FileListVo> queryPage(FileListBo fileListBo);

    /**
    * 文件信息
    * @param id 文件id
    * @return
    */
    FileInfoVo info(Long id);

    /**
     * 新增文件
     * @param file 文件对象
     * @param fileName 文件名
     * @param suffix 文件类型
     * @param sort 排序
     * @param resourceType 来源类型
     * @return
     */
     FileInfoVo save(MultipartFile file, String fileName, String suffix, Integer sort, Integer resourceType, String cosSaveKey);

    /**
     * 修改文件
     * @param fileBo 文件对象
     * @return
     */
    void update(FileBo fileBo);

    /**
     * 删除文件
     * @param id 文件id
     * @return
     */
    void deleteById(Long id);


    /**
     * 更新文件列表
     * @param resourceId 来源id
     * @param resourceType 来源类型 0：用户头像图片 1：商品主图 2：商品详情图 999：未定义
     * @param remarks 描述
     * @param fileIdList 文件id列表
     */
    void updateByFileIds(Long resourceId, Integer resourceType, String remarks, List<Long> fileIdList);

    /**
     * 更新文件信息
     * @param resourceId 来源id
     * @param resourceType 来源类型 0：用户头像图片 1：商品主图 2：商品详情图 999：未定义
     * @param remarks 描述
     * @param fileId 文件id
     */
    void updateOne(Long resourceId, Integer resourceType, String remarks, Long fileId);

    /**
     * 根绝来源id和来源类型获取文件列表
     * @param resourceId 来源id
     * @param resourceType 来源类型 0：用户头像图片 1：商品主图 2：商品详情图 999：未定义
     * @return
     */
    List<FileShowVo> listByResourceIdAndType(Long resourceId, Integer resourceType);

    /**
     * 根绝来源id集合和来源类型获取文件列表
     * @param resourceIdList 来源id集合
     * @param resourceType 来源类型 0：用户头像图片 1：商品主图 2：商品详情图 999：未定义
     * @return
     */
    List<FileShowVo> listByResourceIdListAndType(List<Long> resourceIdList, Integer resourceType);

    /**
     * 根绝来源id和来源类型获取文件
     * @param resourceId 来源id
     * @param resourceType 来源类型 0：用户头像图片 1：商品主图 2：商品详情图 999：未定义
     * @return
     */
    FileShowVo getByResourceIdAndType(Long resourceId, Integer resourceType);

    /**
     * 更新文件列表
     * @param resourceId 来源id
     * @param resourceType 来源类型 0：用户头像图片 1：商品主图 2：商品详情图 999：未定义
     * @param remarks 描述
     * @param fileUpdateBos 文件列表
     */
    void updateList(Long resourceId, Integer resourceType, String remarks, List<FileUpdateBo> fileUpdateBos);

    /**
     * 根据来源id删除所有文件
     * @param resourceId 来源id
     */
    void removeByResourceId(Long resourceId);

    /**
     * 上传压缩文件
     * @param file
     * @return
     */
    R<String> uploadAifuPa(MultipartFile file) throws IOException;

    /**
     * 根绝文件id集合获取文件列表
     * @param fileIds 文件id集合
     * @return
     */
    List<FileShowVo> listByFileIds(Collection<Long> fileIds);

    /**
     * 根绝文件id获取文件信息
     * @param fileId 文件id
     * @return
     */
    FileShowVo infoByFileId(Long fileId);
}

