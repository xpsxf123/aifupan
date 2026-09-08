package com.jiuyu.replay.api.logic.common;

import com.jiuyu.replay.common.bo.FileListBo;
import com.jiuyu.replay.common.bo.UpdateFileBo;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.common.vo.FileInfoVo;
import com.jiuyu.replay.common.vo.FileListVo;
import com.jiuyu.replay.generic.vo.common.FileShowVo;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;


/**
 * 文件
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-07-15 18:18:36
 */
public interface FileLogic {


    /**
     * 文件列表
     * @param fileListBo 文件列表查询参数
     * @return
     */
    R<PageUtils<FileListVo>> queryPage(FileListBo fileListBo);

    /**
    * 文件信息
    * @param id 文件id
    * @return
    */
    R<FileInfoVo> info(Long id);


    /**
     * 修改文件
     * @param updateFileBo 文件对象
     * @return
     */
    R<String> update(UpdateFileBo updateFileBo);

    /**
     * 删除文件
     * @param id 文件id
     * @return
     */
    R<String> delete(Long id);

    /**
     * 根据条件获取单个文件
     * @param resourceId 来源id
     * @param resourceType 来源类型 0：用户头像图片 1：商品主图 2：商品详情图 999：未定义
     * @return
     */
    R<FileShowVo> showOne(Long resourceId, Integer resourceType);

    /**
     * 上传压缩文件
     * @param file
     * @return
     */
    R<String> uploadAifuPa(MultipartFile file) throws IOException;
}

